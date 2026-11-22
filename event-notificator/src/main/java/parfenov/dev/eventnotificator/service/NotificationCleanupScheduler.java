package parfenov.dev.eventnotificator.service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import parfenov.dev.eventnotificator.db.NotificationRepository;
import parfenov.dev.eventnotificator.db.NotificationPayloadRepository;
import java.time.LocalDateTime;
@Component
public class NotificationCleanupScheduler {
    private static final Logger log=LoggerFactory.getLogger(NotificationCleanupScheduler.class);
    private final NotificationRepository notifications;
    private final NotificationPayloadRepository payloads;
    private final NotificationCounterService counters;
    public NotificationCleanupScheduler(NotificationRepository notifications,NotificationPayloadRepository payloads,NotificationCounterService counters){
        this.notifications=notifications;this.payloads=payloads;this.counters=counters;
    }
    @Transactional
    @Scheduled(cron="${notifications.cleanup.cron}")
    public void cleanup() {
        var cutoff=LocalDateTime.now().minusDays(7);
        var affectedUsers=notifications.findUserIdsWithExpiredNotifications(cutoff);
        long deleted=notifications.deleteByCreatedAtBefore(cutoff);
        int removedPayloads=payloads.deleteOrphanedBefore(cutoff);
        if (!affectedUsers.isEmpty()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override public void afterCommit() {
                        affectedUsers.forEach(counters::syncUnreadFromDatabase);
                    }
                }
            );
        }
        if (deleted>0)log.info("Cleaned notifications={} payloads={}",deleted,removedPayloads);
    }
}
