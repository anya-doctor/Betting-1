package pl.maciejpajak.testing.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import pl.maciejpajak.domain.game.Event;
import pl.maciejpajak.exception.BaseEntityNotFoundException;
import pl.maciejpajak.repository.EventRepository;
import pl.maciejpajak.repository.GameRepository;
import pl.maciejpajak.testing.event.event.GameEvent;

@Service
public class EventProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(EventProcessor.class);
    
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;
    
    @Autowired
    private EventRepository eventRepository;
    
    @Autowired
    private GameRepository gameRepository;
    
    public void process(EventDto event) {
        log.debug("System received new event {}", event);
        saveEvent(event);
        applicationEventPublisher.publishEvent(new GameEvent(this, event));
    }
    
    private void saveEvent(EventDto eventDto) {
        eventRepository.save(
                Event.builder()
                .game(gameRepository.findOneByIdAndVisible(eventDto.getGameId(), true)
                        .orElseThrow(() -> new BaseEntityNotFoundException(eventDto.getGameId())))
                .message(eventDto.getMessage())
                .time(eventDto.getTime())
                .value(eventDto.getValue())
                .visible(true)
                .eventType(eventDto.getEventType())
                .build()             
                );
    }

}
