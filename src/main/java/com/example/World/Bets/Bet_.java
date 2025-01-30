package com.example.World.Bets;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.AggregatePath;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.lang.NonNull;

import java.time.LocalDateTime;

public record Bet_(
        @Id
        Long bid,
        @NonNull
        Long tid,
        @Column()
        Integer status,
        Boolean outcome,
        @Positive
        Float amount_for,
        @Positive
        Float amount_against,
        @NotEmpty
        String description,
        @NonNull
        LocalDateTime created_at,
        LocalDateTime deleted_at,
        LocalDateTime ends_at,
        @Version
        Integer b_version

) {
        @Override
        @NonNull
        public LocalDateTime created_at() {
                if(deleted_at() != null){
                        if (created_at.isAfter(deleted_at())){
                                throw new IllegalStateException("created_at cannot be after deleted_at");
                        }
                }else if(ends_at() != null){
                        if (created_at.isAfter(ends_at())){
                                throw new IllegalStateException("created_at cannot be after deleted_at");
                        }
                }

                return created_at;
        }
}
