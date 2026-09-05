package com.example.World.Messages;

public record MessageAction(
        String type,
        Long mid,
        String data
) {
}
