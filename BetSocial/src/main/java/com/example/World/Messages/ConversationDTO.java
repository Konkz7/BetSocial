package com.example.World.Messages;

import jakarta.validation.constraints.NotEmpty;

public record ConversationDTO(
        @NotEmpty
        String name,
        Long uid,
        Message_ lastMessage,
        boolean unread,
        String avatar,
        Long gid
) {
}
