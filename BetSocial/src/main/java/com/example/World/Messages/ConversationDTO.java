package com.example.World.Messages;

import jakarta.validation.constraints.NotEmpty;

public record ConversationDTO(
        @NotEmpty
        String name,
        Long uid,
        /**
         * Whether this is a group rather than a private conversation.
         *
         * Stated rather than left to be inferred. The obvious guess - that uid is
         * null for a group - is wrong: uid names the other person whenever there
         * is exactly one of them, which a named group of two also has. Inferring
         * it would send a group that had shrunk to two people to the wrong screen.
         */
        boolean isGroup,

        MessageView lastMessage,
        boolean unread,
        String avatar,
        Long gid
) {
}
