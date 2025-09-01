package com.example.World.Threads;

import com.example.World.Users.User_;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.data.annotation.Id;
import org.springframework.lang.NonNull;

public record ThreadProfile(
        @Id
        Long tid,
        @NonNull
        User_ user,
        @NotEmpty
        String title ,
        String media ,
        Integer media_type, // 1 is image, 2 is video
        @NotEmpty
        String category,
        @NonNull
        Long likes,
        @NonNull
        boolean liked,
        @NonNull
        Long created_at,
        @NonNull
        Boolean is_private
) {
}
