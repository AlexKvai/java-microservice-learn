package parfenov.dev.eventnotificator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import parfenov.dev.eventnotificator.db.NotificationRepository;

@Service
public class NotificationCounterService {
    private static final Logger log = LoggerFactory.getLogger(NotificationCounterService.class);
    private final StringRedisTemplate redis;
    private final NotificationRepository repository;
    public NotificationCounterService(StringRedisTemplate redis,NotificationRepository repository){
        this.redis=redis;this.repository=repository;
    }
    private String key(Long userId){return "notif:unread:"+userId;}
    public void incrementUnread(Long userId, long delta) {
        if (delta <= 0) return;
        try {
            if (redis.opsForValue().get(key(userId)) == null) syncUnreadFromDatabase(userId);
            else redis.opsForValue().increment(key(userId), delta);
        } catch (RuntimeException ex) { log.warn("Redis unavailable for unread counter, userId={}",userId,ex); }
    }
    public void syncUnreadFromDatabase(Long userId) {
        long count = repository.countByUserIdAndReadFalse(userId);
        try { redis.opsForValue().set(key(userId),Long.toString(count)); }
        catch (RuntimeException ex) { log.warn("Redis SET unread failed, userId={}",userId,ex); }
    }
    public long getUnreadCount(Long userId) {
        try {
            var cached=redis.opsForValue().get(key(userId));
            if (cached != null) return Long.parseLong(cached);
        } catch (RuntimeException ex) { log.warn("Redis unread fallback, userId={}",userId,ex); }
        long count=repository.countByUserIdAndReadFalse(userId);
        try { redis.opsForValue().set(key(userId),Long.toString(count)); }
        catch (RuntimeException ignored) { }
        return count;
    }
}
