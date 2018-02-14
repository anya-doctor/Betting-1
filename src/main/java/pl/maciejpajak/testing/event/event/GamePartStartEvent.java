package pl.maciejpajak.testing.event.event;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;
import pl.maciejpajak.testing.event.EventDto;

@Getter
public class GamePartStartEvent extends GameEvent {

    public GamePartStartEvent(Object source, EventDto eventDto) {
        super(source, eventDto);
    }
//TODO serial version UID


}