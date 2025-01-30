package com.example.World.Bets;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.lang.NonNull;

import java.time.LocalDateTime;

public record BetDTO(

     @NonNull
     Long tid,
     @NotEmpty
     String description,
     Long secondsEndsAt
    ) {
}
