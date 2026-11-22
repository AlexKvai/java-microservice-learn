package parfenov.dev.eventnotificator.db;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="notifications", uniqueConstraints=@UniqueConstraint(name="uk_notification_user_payload",columnNames={"user_id","payload_id"}),
        indexes={@Index(name="idx_notifications_user_read",columnList="user_id,is_read")})
public class NotificationEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(name="user_id",nullable=false)
    private Long userId;
    @Column(name="is_read",nullable=false)
    private boolean read;
    @Column(name="created_at",nullable=false)
    private LocalDateTime createdAt;
    @Column(name="read_at")
    private LocalDateTime readAt;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="payload_id",nullable=false)
    private NotificationEventPayloadEntity payload;
    protected NotificationEntity() {}
    public NotificationEntity(Long userId,LocalDateTime createdAt, NotificationEventPayloadEntity payload) {
        this.userId=userId;this.createdAt=createdAt;this.payload=payload;
    }
    public Long getId(){return id;}
    public Long getUserId(){return userId;}
    public boolean isRead(){return read;}
    public LocalDateTime getCreatedAt(){return createdAt;}
    public NotificationEventPayloadEntity getPayload(){return payload;}
}
