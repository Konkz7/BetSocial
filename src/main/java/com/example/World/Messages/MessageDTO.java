package com.example.World.Messages;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.lang.NonNull;

import java.time.LocalDateTime;

public record MessageDTO(

        Long recipient_id,     // Foreign key to Users table, can be null for group messages
        @NotEmpty
        String description         // The actual message description
) {
}
