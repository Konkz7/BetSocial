package com.example.World.Messages;

public record GroupStatus(

        Long uid,
        Long gid,
        boolean online,
        boolean chatOnline
) {
}
