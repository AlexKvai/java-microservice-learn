package parfenov.dev.eventnotificator.api;
import java.time.LocalDateTime;
import java.util.Map;
public record NotificationDto(Long notificationId,String type,Long eventId,
    LocalDateTime createdAt,boolean isRead,String message,Map<String,Object> payload) {}
