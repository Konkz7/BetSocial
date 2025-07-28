package com.example.World.Groups;

import org.springframework.data.annotation.Id;
import org.springframework.lang.NonNull;

public record Groupuser_(

    @Id
    Long guid,
    @NonNull
    Long gid,
    @NonNull
    Long uid,
    @NonNull
    Boolean administrator


) {
}
