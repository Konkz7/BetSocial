package com.example.World.Threads;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.lang.NonNull;

public record ThreadDTO(
        @NotEmpty
        String title ,
        String description ,
        @NonNull
        Boolean is_private,
        @NotEmpty
        String category
) {
}
