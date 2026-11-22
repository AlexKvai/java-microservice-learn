package parfenov.dev.eventmanager.events.domain;

import java.time.LocalDateTime;
import java.util.List;

public record Event(
        Long id,
        String name,
        Long ownerId,
        int maxPlaces,
        List<EventRegistration> registrationList,
        LocalDateTime date,
        int cost,
        int duration,
        Long locationId,
        EventStatus status
) implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
}
