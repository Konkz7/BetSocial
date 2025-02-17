package com.example.World.Bets;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.lang.NonNull;

public record Betsave_(
        @Id
        Long bsid,
        @NonNull
        Long bid,
        @NonNull
        Long uid
) {
}
