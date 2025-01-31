package com.example.World.Friends;


import org.springframework.data.annotation.Id;
import org.springframework.lang.NonNull;

public record Friendship_(
        @Id
        Long fid,
        @NonNull
        Long request_id,
        @NonNull
        Long receive_id,

        Integer gid,
        @NonNull
        Integer stage
) {
}
