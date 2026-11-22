package parfenov.dev.eventnotificator.db;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_event_payloads", uniqueConstraints = @UniqueConstraint(name="uk_payload_message", columnNames="message_id"))
public class NotificationEventPayloadEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name="message_id", nullable=false)
    private UUID messageId;
    @Column(name="event_type", nullable=false)
    private String eventType;
    @Column(name="event_id", nullable=false)
    private Long eventId;
    @Column(name="occurred_at", nullable=false)
    private LocalDateTime occurredAt;
    @Column(name="owner_id")
    private Long ownerId;
    @Column(name="changed_by_id")
    private Long changedById;
    @Column(name="payload_json", columnDefinition="text", nullable=false)
    private String payloadJson;
    protected NotificationEventPayloadEntity() {}
    public NotificationEventPayloadEntity(UUID messageId,String eventType,Long eventId,
                LocalDateTime occurredAt,Long ownerId,Long changedById,String payloadJson) {
        this.messageId=messageId;this.eventType=eventType;this.eventId=eventId;
        this.occurredAt=occurredAt;this.ownerId=ownerId;this.changedById=changedById;this.payloadJson=payloadJson;
    }
    public Long getId(){return id;}
    public UUID getMessageId(){return messageId;}
    public String getEventType(){return eventType;}
    public Long getEventId(){return eventId;}
    public LocalDateTime getOccurredAt(){return occurredAt;}
    public String getPayloadJson(){return payloadJson;}
}
