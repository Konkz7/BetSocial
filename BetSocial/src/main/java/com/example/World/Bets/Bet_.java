package com.example.World.Bets;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.AggregatePath;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.lang.NonNull;


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
        Long created_at,
        Long deleted_at,
        @NonNull
        Long ends_at,
        @NonNull
        Boolean is_verified,
        @NonNull
        Boolean king_mode,
        @NonNull
        Boolean profit_mode,
        @NonNull
        Float max_amount,
        @NonNull
        Float min_amount,
        @Version
        Integer b_version

) {
}
