package pl.maciejpajak.api;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pl.maciejpajak.api.dto.BetOptionWithOddDto;
import pl.maciejpajak.api.dto.CouponPlaceDto;
import pl.maciejpajak.api.dto.CouponShowDto;
import pl.maciejpajak.api.dto.GroupCouponPlaceDto;
import pl.maciejpajak.api.temp.CouponService;
import pl.maciejpajak.security.CurrentUser;

@RestController
@RequestMapping("/coupons")
public class CouponApi {
    
    private final CouponService couponService;
    
    public CouponApi(CouponService couponService) {
        this.couponService = couponService;
    }

    // TODO remove
    @GetMapping("/test")
    public CouponPlaceDto test() {
        CouponPlaceDto c = new CouponPlaceDto();
        c.setAmount(BigDecimal.valueOf(100.33));
        c.setOddsChangeAccepted(true);
        
        
        List<BetOptionWithOddDto> list = new ArrayList<>();
        BetOptionWithOddDto b1 = new BetOptionWithOddDto();
        b1.setBetOptionId(1L);
        b1.setBetOptionId(2L);
        list.add(b1);
        c.setBetOptionsWithOdds(list);
        return c;
    }
    
    @GetMapping("/all")
    public Collection<CouponShowDto> findAllCoupons(@AuthenticationPrincipal CurrentUser user) {
        // TODO 
        Long id = 1L;
        return couponService.findAllForCurrentUser(id);
    }
    
    @PostMapping("/create")
    public void createCoupon(@RequestBody @Valid CouponPlaceDto couponDto, @AuthenticationPrincipal CurrentUser principal) {
        couponService.createCoupon(couponDto, principal.getId());
    }
    
    @PostMapping("/group/create")
    public void createGroupCoupon(@RequestBody @Valid GroupCouponPlaceDto couponDto, @AuthenticationPrincipal CurrentUser principal) {
        couponService.createCoupon(couponDto, principal.getId());
    }
    
//    @GetMapping("/show/{id}") // TODO
//    public CouponShowDto showCouponById(@PathVariable Long id, @AuthenticationPrincipal CurrentUser user) {
//        
//    }

}
