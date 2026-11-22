package parfenov.dev.eventmanager.cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class EventCacheInvalidator {
    private static final Logger log = LoggerFactory.getLogger(EventCacheInvalidator.class);
    private final CacheManager cacheManager;
    public EventCacheInvalidator(CacheManager cacheManager) { this.cacheManager = cacheManager; }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onEventChanged(EventCacheInvalidation marker) {
        evict(marker.eventId());
    }
    public void evict(Long id) {
        try {
            var cache = cacheManager.getCache("events");
            if (cache != null) cache.evict("id:" + id);
        } catch (RuntimeException ex) {
            log.warn("Redis unreachable while invalidating event {}", id, ex);
        }
    }
    public record EventCacheInvalidation(Long eventId) {}
}
