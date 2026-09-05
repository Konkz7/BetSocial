package com.example.World.Notifications;

public record NotificationDTO(
        Long actor_id,
        String notification_type,
        Long target_id,
        String target_type

) {
}
