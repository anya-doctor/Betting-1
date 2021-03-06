package pl.maciejpajak.domain.bet;

import java.util.Set;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;
import pl.maciejpajak.domain.game.Game;
import pl.maciejpajak.domain.util.BetLastCall;

@Entity
@Getter
@Setter
public class Bet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Access(AccessType.PROPERTY)
    protected Long id;
    
    @JsonIgnore
    private boolean visible = true;
    
    @ManyToOne
    @JsonIgnore(value = false)
    private Game game;
    
    @OneToMany(mappedBy = "bet", fetch = FetchType.EAGER)
    private Set<BetOption> betOptions;
    
    private String description;
    
    @Enumerated(EnumType.STRING)
    private BetLastCall lastCall;
    
    private boolean betable;
    
}
