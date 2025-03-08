package com.example.World.Cards;

import org.springframework.data.annotation.Id;
import org.springframework.lang.NonNull;

public record Card_(
        @Id
        String card_id,
        @NonNull
        Long user_id,
        @NonNull
        Long created_at
) {
}
