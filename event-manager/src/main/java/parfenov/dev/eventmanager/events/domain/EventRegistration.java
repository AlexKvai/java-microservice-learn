package parfenov.dev.eventmanager.events.domain;

public record EventRegistration(
        Long id,
        Long userId,
        Long eventId
) implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
}
