package com.example.World.Messages;

import java.util.List;

/**
 * One page of a conversation, and how to ask for the messages before it.
 *
 * Newest first, matching the order an inverted list renders in - the client
 * shows index 0 at the bottom, so the newest message needs to be first rather
 * than last. Reversing it here and again on the client would be two chances to
 * get it wrong.
 *
 * has_more is the server's answer rather than something inferred from a short
 * page, for the same reason as the feed: a client cannot tell a short page apart
 * from the start of the conversation.
 */
public record MessagePage(

        List<MessageView> messages,

        /** Pass back to fetch the messages older than this page. Null at the start. */
        Long next_cursor_created_at,
        Long next_cursor_mid,

        boolean has_more
) {
}
