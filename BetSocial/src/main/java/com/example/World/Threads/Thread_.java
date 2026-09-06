package com.example.World.Threads;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.lang.NonNull;





public record Thread_(
        @Id
        Long tid,
        @NonNull
        Long uid,
        @NotEmpty
        String title ,
        String media ,
        Integer media_type, // 1 is image, 2 is video
        @NotEmpty
        String category,
        @NonNull
        Long likes,
        @NonNull
        Long created_at,
        Long deleted_at,
        @NonNull
        Boolean is_private,
        @Version
        Integer t_version
) {
}
