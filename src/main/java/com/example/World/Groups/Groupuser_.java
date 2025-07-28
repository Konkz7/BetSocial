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
    Long created_at,
    @NonNull
    Long last_read_timestamp,
    @NonNull
    Boolean administrator


) {
}
