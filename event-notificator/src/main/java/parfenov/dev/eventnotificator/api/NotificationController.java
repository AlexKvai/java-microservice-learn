package parfenov.dev.eventnotificator.api;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import parfenov.dev.eventnotificator.security.JwtUser;
import parfenov.dev.eventnotificator.service.NotificationCounterService;
import parfenov.dev.eventnotificator.service.NotificationService;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService service;
    private final NotificationCounterService counters;

    public NotificationController(NotificationService service, NotificationCounterService counters) {
        this.service = service;
        this.counters = counters;
    }

    @GetMapping
    public List<NotificationDto> unread() {
        return service.unread(currentUserId());
    }

    @PostMapping
    public ResponseEntity<Void> markRead(@Valid @RequestBody MarkNotificationsReadRequest request) {
        service.markRead(currentUserId(), request.notificationIds());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse count() {
        return new UnreadCountResponse(counters.getUnreadCount(currentUserId()));
    }

    private Long currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof JwtUser user)) {
            throw new InsufficientAuthenticationException("Missing JWT user");
        }
        return user.id();
    }
}
