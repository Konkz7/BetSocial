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
        @Positive
        Float amount_bet ,
        Float amount_won ,
        @NonNull
        Long created_at,
        Long deleted_at,
        @Version
        Integer p_version // Version number for optimistic locking
) {
        @Override
        @NonNull
        public Long created_at() {
                if(deleted_at() != null){
                        if (created_at >= deleted_at){
                                throw new IllegalStateException("created_at cannot be after deleted_at");
                        }
                }

                return created_at;
        }
}
