package com.example.World.Comments;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.lang.NonNull;

public record Commentlike_(
        @Id
        Long clid,
        @NonNull
        Long cid,
        @NonNull
        Long uid
) {
}
