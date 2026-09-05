package com.example.World.Messages;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.lang.NonNull;



public record MessageDTO(

        Long recipient_id,     // Foreign key to Users table, can be null for group messages
        Long gid,
        @NotEmpty
        String description ,        // The actual message description
        @NonNull
        Integer media_type
) {
}
