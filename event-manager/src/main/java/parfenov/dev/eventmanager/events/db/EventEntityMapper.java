package parfenov.dev.eventmanager.events.db;

import org.springframework.stereotype.Component;
import parfenov.dev.eventmanager.events.domain.Event;
import parfenov.dev.eventmanager.events.domain.EventRegistration;

@Component
public class EventEntityMapper {

    public Event toDomain(EventEntity eventEntity) {
        return new Event(
                eventEntity.getId(),
                eventEntity.getName(),
                eventEntity.getOwnerId(),
                eventEntity.getMaxPlaces(),
                eventEntity.getRegistrationList().stream()
                        .map(it -> new EventRegistration(
                                it.getId(),
                                it.getUserId(),
                                eventEntity.getId()
                        ))
                        .toList(),
                eventEntity.getDate(),
                eventEntity.getCost(),
                eventEntity.getDuration(),
                eventEntity.getLocationId(),
                eventEntity.getStatus()
        );
    }

}
