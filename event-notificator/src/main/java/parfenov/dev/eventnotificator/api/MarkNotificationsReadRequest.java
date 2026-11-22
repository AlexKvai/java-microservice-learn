package parfenov.dev.eventnotificator.api;
import jakarta.validation.constraints.NotNull;
import java.util.List;
public record MarkNotificationsReadRequest(@NotNull List<Long> notificationIds) {}
