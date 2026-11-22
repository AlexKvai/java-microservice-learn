package parfenov.dev.eventnotificator.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import parfenov.dev.eventcommon.EventChangeKafkaMessage;
import parfenov.dev.eventnotificator.api.NotificationDto;
import parfenov.dev.eventnotificator.db.NotificationEntity;
import parfenov.dev.eventnotificator.db.NotificationEventPayloadEntity;
import parfenov.dev.eventnotificator.db.NotificationRepository;
import parfenov.dev.eventnotificator.db.NotificationPayloadRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class NotificationService {
    private static final Logger log=LoggerFactory.getLogger(NotificationService.class);
    private final NotificationPayloadRepository payloads;
    private final NotificationRepository notifications;
    private final NotificationCounterService counters;
    private final ObjectMapper mapper=new ObjectMapper().registerModule(new JavaTimeModule());
    public NotificationService(NotificationPayloadRepository payloads, NotificationRepository notifications,
                               NotificationCounterService counters) {
        this.payloads=payloads;this.notifications=notifications;this.counters=counters;
    }
    @Transactional
    public void consume(EventChangeKafkaMessage message,String originalJson) {
        if(message.messageId()==null || message.eventId()==null) {
            log.warn("Malformed Kafka event, missing id"); return;
        }
        if(payloads.existsByMessageId(message.messageId())) {
            log.info("Ignoring duplicate Kafka message {}",message.messageId());return;
        }
        var payload=payloads.save(new NotificationEventPayloadEntity(message.messageId(),message.eventType(),
            message.eventId(),message.occurredAt(),message.ownerId(),message.changedById(),originalJson));
        var subscribers=message.subscribers()==null? List.<Long>of():message.subscribers().stream()
            .filter(Objects::nonNull).distinct().toList();
        for(var userId:subscribers)
            notifications.save(new NotificationEntity(userId,LocalDateTime.now(),payload));
        afterCommit(()->subscribers.forEach(userId->counters.incrementUnread(userId,1)));
    }
    @Transactional(readOnly=true)
    public List<NotificationDto> unread(Long userId) {
        return notifications.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId)
            .stream().map(n -> {
                var payload=n.getPayload();
                try {
                    Map<String,Object> data=mapper.readValue(payload.getPayloadJson(),new TypeReference<>() {});
                    Object occurredAt = data.get("occurredAt");
                    if (occurredAt instanceof List<?>) {
                        data.put("occurredAt", mapper.convertValue(occurredAt, LocalDateTime.class).toString());
                    }
                    return new NotificationDto(n.getId(),payload.getEventType(),payload.getEventId(),n.getCreatedAt(),
                        n.isRead(),"Event #"+payload.getEventId()+" changed",data);
                } catch(Exception e) {throw new IllegalStateException("Invalid stored notification payload",e);}
            }).toList();
    }
    @Transactional
    public void markRead(Long userId,List<Long> ids) {
        if(ids==null || ids.isEmpty()) return;
        var unique=ids.stream().filter(Objects::nonNull).distinct().toList();
        if(unique.isEmpty()) return;
        notifications.markAsRead(userId,unique,LocalDateTime.now());
        afterCommit(()->counters.syncUnreadFromDatabase(userId));
    }
    private void afterCommit(Runnable action) {
        if(TransactionSynchronizationManager.isSynchronizationActive())
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
                @Override public void afterCommit(){action.run();}
            });
        else action.run();
    }
}
