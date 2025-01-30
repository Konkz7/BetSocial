package com.example.World.Predictions;

import jakarta.validation.constraints.Positive;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.lang.NonNull;

import java.time.LocalDateTime;

public record PredictionDTO(

        @NonNull
        Long bid,
        @NonNull
        Boolean prediction ,
        @Positive
        Float amount_bet
) {
}
