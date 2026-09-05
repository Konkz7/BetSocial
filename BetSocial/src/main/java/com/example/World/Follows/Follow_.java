package com.example.World.Follows;


import org.springframework.data.annotation.Id;
import org.springframework.lang.NonNull;

public record Follow_(
        @Id
        Long fid,
        @NonNull
        Long request_id,
        @NonNull
        Long receive_id
) {
}
