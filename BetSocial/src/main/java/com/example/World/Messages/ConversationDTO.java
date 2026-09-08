package com.example.World.Messages;

import jakarta.validation.constraints.NotEmpty;

public record ConversationDTO(
        @NotEmpty
        String name,
        Long uid,
        MessageView lastMessage,
        boolean unread,
        String avatar,
        Long gid
) {
}
