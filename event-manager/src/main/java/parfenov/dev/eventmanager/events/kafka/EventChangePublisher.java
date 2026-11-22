package parfenov.dev.eventmanager.events.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import parfenov.dev.eventcommon.EventChangeKafkaMessage;

@Component
public class EventChangePublisher {
    private static final Logger log = LoggerFactory.getLogger(EventChangePublisher.class);
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final String topic;

    public EventChangePublisher(KafkaTemplate<String,String> kafka, @Value("${events.kafka.topic}") String topic) {
        this.kafka = kafka;
        this.topic = topic;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void publish(EventChangeKafkaMessage message) {
        try {
            var body = mapper.writeValueAsString(message);
            kafka.send(topic, message.eventId().toString(), body)
                .whenComplete((result, error) -> {
                    if (error != null) log.error("Kafka publish failed messageId={}", message.messageId(), error);
                    else log.info("Kafka event published messageId={} eventId={}", message.messageId(), message.eventId());
                });
        } catch (Exception e) {
            log.error("Unable to publish event change {}", message.messageId(), e);
        }
    }
}
