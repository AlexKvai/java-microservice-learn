package parfenov.dev.eventmanager.events.api;

import org.springframework.stereotype.Component;
import parfenov.dev.eventmanager.events.db.EventEntity;
import parfenov.dev.eventmanager.events.domain.Event;
import parfenov.dev.eventmanager.events.domain.EventRegistration;

@Component
public class EventDtoMapper {

    public EventDto toDto(Event event) {
        return new EventDto(
                event.id(),
                event.name(),
                event.ownerId(),
                event.maxPlaces(),
                event.registrationList().size(),
                event.date(),
                event.cost(),
                event.duration(),
                event.locationId(),
                event.status()
        );
    }
    
}
