package com.example.World.Bets;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.lang.NonNull;



public record BetDTO(

     @NonNull
     Long tid,
     @NotEmpty
     String description,
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
     Float min_amount
    ) {
}
