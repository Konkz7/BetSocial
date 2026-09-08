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
        // Whole coins. These were floats, which drift as a pool is added to and
        // cannot hold most decimal amounts exactly - the wrong type for a number
        // that decides who gets paid. @Positive was also wrong: a pool starts at
        // zero and stays there until somebody stakes on that side.
        @NonNull
        Long amount_for,
        @NonNull
        Long amount_against,
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
        /** The largest single stake allowed, or 0 for no limit. */
        @NonNull
        Long max_amount,
        /** The smallest single stake allowed. 0 for no limit. */
        @NonNull
        Long min_amount,
        @Version
        Integer b_version

) {
}
