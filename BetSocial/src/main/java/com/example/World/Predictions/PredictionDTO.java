package com.example.World.Predictions;

import jakarta.validation.constraints.Positive;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.lang.NonNull;



public record PredictionDTO(

        @NonNull
        Long bid,
        @NonNull
        Boolean prediction ,
        @Positive
        Float amount_bet
) {
}
