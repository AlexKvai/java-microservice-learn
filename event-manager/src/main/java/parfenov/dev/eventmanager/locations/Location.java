package parfenov.dev.eventmanager.locations;

public record Location(
        Long id,
        String name,
        String address,
        Long capacity,
        String description
) implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
}
