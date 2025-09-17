package com.example.World.Notifications;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends ListCrudRepository<Notification_,Long> {

    @Query(value = "SELECT * FROM Notification_ WHERE uid = :uid AND is_deleted = false ORDER BY created_at DESC")
    List<Notification_> getActiveNotifications(@Param("uid") Long uid );


    @Query("""
    SELECT * FROM Notification_
    WHERE notification_type = :type
      AND actor_id = :actorId
      AND target_id = :targetId
      AND is_deleted = false
    ORDER BY created_at DESC
    """)
    Optional<Notification_> findLatestNonDeleted(
            @Param("type") String type,
            @Param("actorId") Long actorId,
            @Param("targetId") Long targetId
    );

    @Modifying
    @Transactional
    @Query("UPDATE Notification_ SET  is_deleted = true WHERE nid = :id")
    int remove(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE Notification_ SET body = :body , created_at = :now WHERE nid = :id")
    int changeContent(@Param("id") Long id , @Param("body") String body , @Param("now") Long now);


    @Modifying
    @Transactional
    @Query("UPDATE Notification_ SET is_read = true WHERE nid = :id")
    int updateReadMarkers(@Param("id") Long id );

}
