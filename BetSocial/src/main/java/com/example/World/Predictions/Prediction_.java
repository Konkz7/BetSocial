package com.example.World.Predictions;

import jakarta.validation.constraints.Positive;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.lang.NonNull;



public record Prediction_(
        @Id
        Long pid,
        @NonNull
        Long bid,
        @NonNull
        Long uid,
        @NonNull
        Boolean prediction , //true for, false against
        /** Whole coins, taken from the wallet the moment the prediction is placed. */
        @NonNull
        Long amount_bet ,
        Long amount_won ,
        @NonNull
        Long created_at,
        Long deleted_at,
        @Version
        Integer p_version // Version number for optimistic locking
) {
}
