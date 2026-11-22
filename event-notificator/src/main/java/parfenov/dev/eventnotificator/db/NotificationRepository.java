package parfenov.dev.eventnotificator.db;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface NotificationRepository extends JpaRepository<NotificationEntity,Long> {
    List<NotificationEntity> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndReadFalse(Long userId);
    long deleteByCreatedAtBefore(LocalDateTime cutoff);
    @Query("SELECT DISTINCT n.userId FROM NotificationEntity n WHERE n.createdAt < :cutoff")
    List<Long> findUserIdsWithExpiredNotifications(@Param("cutoff") LocalDateTime cutoff);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE NotificationEntity n SET n.read=true, n.readAt=:now WHERE n.userId=:userId AND n.id IN :ids AND n.read=false")
    int markAsRead(@Param("userId") Long userId, @Param("ids") List<Long> ids, @Param("now") LocalDateTime now);
}
