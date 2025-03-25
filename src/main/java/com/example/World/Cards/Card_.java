package com.example.World.Cards;

import org.springframework.data.annotation.Id;
import org.springframework.lang.NonNull;

public record Card_(
        @Id
        String card_id,
        @NonNull
        Long uid,
        @NonNull
        Long created_at
) {
}
