package com.example.World.Threads;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.lang.NonNull;

public record Threadlike_(
        @Id
        Long tlid,
        @NonNull
        Long tid,
        @NonNull
        Long uid
) {
}
