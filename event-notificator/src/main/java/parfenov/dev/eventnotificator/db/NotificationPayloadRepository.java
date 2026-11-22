package parfenov.dev.eventnotificator.db;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface NotificationPayloadRepository extends JpaRepository<NotificationEventPayloadEntity,Long> {
    boolean existsByMessageId(UUID messageId);
    @Modifying(flushAutomatically=true)
    @Query("DELETE FROM NotificationEventPayloadEntity p WHERE p.occurredAt < :cutoff AND NOT EXISTS (SELECT n.id FROM NotificationEntity n WHERE n.payload.id=p.id)")
    int deleteOrphanedBefore(@Param("cutoff") LocalDateTime cutoff);
}
