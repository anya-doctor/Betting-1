package pl.maciejpajak.domain.game;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.validator.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class PlayingParty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;
    
    @JsonIgnore
    private boolean visible = true;
    
    @NotBlank
    private String name;
    
    @NotBlank
    private String description;
    
    @ManyToOne
    @JoinColumn(name = "sport_id")
    private Sport sport;
    
}
