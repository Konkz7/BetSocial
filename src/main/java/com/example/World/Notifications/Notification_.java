package com.example.World.Notifications;

import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import reactor.util.annotation.NonNull;

public record Notification_(
        @Id
        Long nid,
        @NonNull
        Long uid,

        Long actor_id,
        @NonNull
        @Size(max = 50)
        String notification_type,

        Long target_id,
        @Size(max = 50)
        String target_type,

        String description,

        boolean is_read,

        boolean is_deleted,
        @NonNull
        Long created_at
) {
}
