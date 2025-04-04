package com.example.World.Notifications;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface NotificationRepository extends ListCrudRepository<Notification_,Long> {

    @Query(value = "SELECT * FROM Notification_ WHERE uid = :uid AND is_deleted = false")
    List<Notification_> getActiveNotifications(@Param("uid") Long uid );

    @Modifying
    @Transactional
    @Query("UPDATE Notification_ SET  is_deleted = true WHERE nid = :id")
    int remove(@Param("id") Long id);

}
