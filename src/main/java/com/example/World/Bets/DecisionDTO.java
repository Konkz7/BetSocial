package com.example.World.Bets;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.lang.NonNull;

public record DecisionDTO(

        @NonNull
        Long bid,
        @NonNull
        Boolean decision,
        @NotEmpty
        String reason
) {
}
