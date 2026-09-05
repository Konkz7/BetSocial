package com.example.World.Threads;

import com.example.World.Users.User_;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.lang.NonNull;

public record ThreadDTO(
        @NotEmpty
        String title ,
        String media ,
        Integer media_type,
        @NonNull
        Boolean is_private,
        @NotEmpty
        String category
) {
}
