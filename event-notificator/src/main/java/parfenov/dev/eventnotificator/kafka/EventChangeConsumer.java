package parfenov.dev.eventnotificator.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import parfenov.dev.eventcommon.EventChangeKafkaMessage;
import parfenov.dev.eventnotificator.service.NotificationService;

@Component
public class EventChangeConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventChangeConsumer.class);

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final NotificationService notifications;

    public EventChangeConsumer(NotificationService notifications) {
        this.notifications = notifications;
    }

    @KafkaListener(topics = "${events.kafka.topic}")
    public void consume(String json) {
        try {
            var message = mapper.readValue(json, EventChangeKafkaMessage.class);
            notifications.consume(message, json);
        } catch (JsonProcessingException bad) {
            log.error("Skipping invalid Kafka JSON message", bad);
        }
    }
}
