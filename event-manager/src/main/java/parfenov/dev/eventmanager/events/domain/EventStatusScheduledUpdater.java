package parfenov.dev.eventmanager.events.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import parfenov.dev.eventcommon.ChangeItem;
import parfenov.dev.eventcommon.EventChangeKafkaMessage;
import parfenov.dev.eventmanager.cache.EventCacheInvalidator;
import parfenov.dev.eventmanager.events.db.EventRegistrationRepository;
import parfenov.dev.eventmanager.events.db.EventRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Configuration
@EnableScheduling
public class EventStatusScheduledUpdater {
    private static final Logger log = LoggerFactory.getLogger(EventStatusScheduledUpdater.class);
    private final EventRepository events;
    private final EventRegistrationRepository registrations;
    private final EventCacheInvalidator invalidator;
    private final ApplicationEventPublisher publisher;
    public EventStatusScheduledUpdater(EventRepository events, EventRegistrationRepository registrations,
                                       EventCacheInvalidator invalidator, ApplicationEventPublisher publisher) {
        this.events = events; this.registrations = registrations;
        this.invalidator = invalidator; this.publisher = publisher;
    }
    @Scheduled(cron = "${event.status.cron}")
    public void updateEventStatuses() {
        changeStatuses(events.findStartedEventsWithStatus(EventStatus.WAIT_START), EventStatus.WAIT_START, EventStatus.STARTED);
        changeStatuses(events.findEndedEventsWithStatus(EventStatus.STARTED), EventStatus.STARTED, EventStatus.FINISHED);
    }
    private void changeStatuses(List<Long> eventIds, EventStatus previous, EventStatus next) {
        for (Long id : eventIds) {
            var entity = events.findById(id).orElse(null);
            if (entity == null || entity.getStatus() != previous) continue;
            events.changeEventStatus(id, next);
            invalidator.evict(id);
            publisher.publishEvent(new EventChangeKafkaMessage(UUID.randomUUID(), "EVENT_STATUS_CHANGED", id,
                LocalDateTime.now(), entity.getOwnerId(), null, registrations.findSubscribers(id),
                List.of(new ChangeItem("status", previous.name(), next.name())), entity.getName()));
            log.info("Event {} status changed {} -> {}", id, previous, next);
        }
    }
}
