package com.example.World.Threads;

import jakarta.validation.constraints.NotEmpty;

public record ThreadDTO(
        @NotEmpty
        String title ,
        String description ,
        @NotEmpty
        String category
) {
}
