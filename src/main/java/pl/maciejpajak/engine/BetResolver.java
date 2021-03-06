package pl.maciejpajak.engine;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import pl.maciejpajak.domain.bet.Bet;
import pl.maciejpajak.domain.bet.BetOption;
import pl.maciejpajak.domain.bet.PlacedBet;
import pl.maciejpajak.domain.coupon.Coupon;
import pl.maciejpajak.domain.coupon.GroupCoupon;
import pl.maciejpajak.domain.game.Game;
import pl.maciejpajak.domain.offers.BidAmountBonus;
import pl.maciejpajak.domain.user.TransactionType;
import pl.maciejpajak.domain.user.User;
import pl.maciejpajak.domain.util.BetOptionStatus;
import pl.maciejpajak.domain.util.CouponStatus;
import pl.maciejpajak.domain.util.NotificationType;
import pl.maciejpajak.event.NotifyUserEvent;
import pl.maciejpajak.repository.BetOptionRepository;
import pl.maciejpajak.repository.BidAmountBonusRepository;
import pl.maciejpajak.repository.CouponRepository;
import pl.maciejpajak.service.TransactionService;

@Component
public class BetResolver {

    private static final Logger log = LoggerFactory.getLogger(BetResolver.class);
   
    @Autowired
    private BetOptionRepository betOptionRepository;
    
    @Autowired
    private CouponRepository couponRepository;
    
    @Autowired
    private TransactionService transactionService;
    
    @Autowired
    private BidAmountBonusRepository bidAmountBonusRepository;
    
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    /**
     * Resolves all bets for {@code game} in {@code bets}
     * Also updates coupons with those bets, resolve them if necessary and pays out prizes
     * @param game
     * @param bets
     * @throws ScriptException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    @Async
    @Transactional(rollbackOn = {})
    public void resolve(Game game, Collection<Bet> bets) throws ScriptException, IllegalAccessException, InvocationTargetException {

        Long start = System.nanoTime();

        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine engine = mgr.getEngineByName("JavaScript");
        
        List<BetOption> betOptions = betOptionRepository.findAllByBetInAndVisible(bets, true);

        for (BetOption bo : betOptions) {
            String winCondition = bo.getWinCondition();
            Pattern r = Pattern.compile("\\{([^\\}]+)\\}");
            Matcher m = r.matcher(winCondition);
            while (m.find( )) {
               Object o = game;
               String[] names = m.group(1).split("\\.");
               for (int j = 0 ; j < names.length - 1 ; j++) {
                   String name = names[j+1];
                   Method method = Arrays.asList(o.getClass().getMethods()).stream()
                           .filter(meth -> meth.getName().equalsIgnoreCase("get" + name)).findFirst().orElseThrow(() -> new RuntimeException());
                   o = method.invoke(o, null);
               }
               winCondition = winCondition.replace(m.group(0), o.toString());               
            }
            boolean isWinConditionSatisfied = (boolean) engine.eval(winCondition);
            log.debug("initial win condition: {}", bo.getWinCondition());
            log.debug("parsed win condition: {}", winCondition);
            log.debug("win result: {}", isWinConditionSatisfied);
            
            bo.setStatus(isWinConditionSatisfied ? BetOptionStatus.WON : BetOptionStatus.LOST);
            
            updateCouponByBetOption(bo);
        }
        betOptionRepository.save(betOptions);
        
        Long stop = (System.nanoTime() - start) / 1000000;
        log.debug("bets resolving time {} ms", stop);
    }
    
    private void updateCouponByBetOption(BetOption betOption) {
        
        Collection<Coupon> coupons = couponRepository.findAllByStatusAndPlacedBetsBetOptionIdAndVisible(CouponStatus.PLACED, betOption.getId(), true);
        log.debug("found coupons {} for betOption id = {}", coupons.size(), betOption.getId());
        coupons.forEach(c -> {
            log.debug("setting unsersolvedBetsCount for coupon (id = {}) to {}", c.getId(), c.getUnsersolvedBetsCount() - 1); 
            c.setUnsersolvedBetsCount(c.getUnsersolvedBetsCount() - 1);
            couponRepository.save(c);
            if (c.getUnsersolvedBetsCount() == 0) {
                resolveCoupon(c);
            } 
        });
    }

    private void resolveCoupon(Coupon coupon) {
        log.debug("resolving coupon (id = {})", coupon.getId());
        if (isCouponWon(coupon)) {
            log.debug("coupon (id = {}) WON, going go pay out prize", coupon.getId());
            coupon.setStatus(CouponStatus.WON);
            payOutPrize(coupon);
        } else {
            log.debug("coupon (id = {}) LOST", coupon.getId());
            coupon.setStatus(CouponStatus.WON);
            coupon.setBonus(BigDecimal.ZERO);
            coupon.setTotalPrize(BigDecimal.ZERO);
            if (coupon instanceof GroupCoupon) {
                ((GroupCoupon) coupon).getIntivations().forEach(inv -> {
                    applicationEventPublisher.publishEvent(new NotifyUserEvent(this, inv.getInvitedUser().getId(), coupon.getId(), NotificationType.COUPON_LOST));
                });
            } 
            applicationEventPublisher.publishEvent(new NotifyUserEvent(this, coupon.getOwner().getId(), coupon.getId(), NotificationType.COUPON_LOST));
        }
        log.debug("saving coupon (id = {})", coupon.getId());
        couponRepository.save(coupon); 
    }

    private boolean isCouponWon(Coupon coupon) {
        log.debug("checking id coupon (id = {}) is won", coupon.getId());
        boolean isCouponWon = true;
        for (PlacedBet pb : coupon.getPlacedBets()) {
            if(pb.getBetOption().getStatus().equals(BetOptionStatus.LOST)) {
                isCouponWon = false;
                break;
            }
        }
        return isCouponWon;
    }

    private void payOutPrize(Coupon coupon) {
        log.debug("inside payOutPrize for coupon (id = {})", coupon.getId());
        BigDecimal totalPrize = coupon.getValue();
        for (PlacedBet pb : coupon.getPlacedBets()) {
            totalPrize = totalPrize.multiply(pb.getOdd().getValue());
        }
        BigDecimal bonus = bidAmountBonusRepository
                .findTopByMinimalBidIsLessThanEqualAndVisibleOrderByMinimalBidDesc(totalPrize, true)
                .orElse(new BidAmountBonus()).getRelativeRevenuBonus();
        totalPrize = totalPrize.multiply(bonus.add(BigDecimal.ONE)).setScale(2, RoundingMode.CEILING);
        coupon.setBonus(bonus);
        coupon.setTotalPrize(totalPrize);
        log.debug("bonus: {}", bonus);
        log.debug("total prize: {}", totalPrize);
        if (coupon instanceof GroupCoupon) {
            Map<User, BigDecimal> usersAmounts = new HashMap<>();
            usersAmounts.put(coupon.getOwner(), coupon.getOwnerTransaction().getAmount());
            ((GroupCoupon) coupon).getIntivations().forEach(inv -> {
                usersAmounts.put(inv.getInvitedUser(), inv.getBetTransaction().getAmount().negate());
                applicationEventPublisher.publishEvent(new NotifyUserEvent(this, inv.getInvitedUser().getId(), coupon.getId(), NotificationType.COUPON_WON));
            });
            usersAmounts.forEach((u, a) -> 
                transactionService.createTransaction(coupon.getTotalPrize().multiply(a).divide(coupon.getValue()).setScale(2, RoundingMode.CEILING), u, TransactionType.WIN));
        } else {
            transactionService.createTransaction(coupon.getTotalPrize(), coupon.getOwner(), TransactionType.WIN);
        }
        applicationEventPublisher.publishEvent(new NotifyUserEvent(this, coupon.getOwner().getId(), coupon.getId(), NotificationType.COUPON_WON));
    }
    
}
