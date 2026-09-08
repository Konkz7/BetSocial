package com.example.World.Messages;

/**
 * A message as the client sees it.
 *
 * Identical to {@link Message_} except that {@code is_read} is worked out when the
 * message is read rather than stored on it. It means "everyone else in this
 * conversation has read this", which is what the sender's seen-tick shows.
 *
 * It has to be derived. A column on the message could only ever hold one
 * recipient's state, so in a conversation of five it was answering a question
 * about one of them and silently standing in for the rest. Read state belongs to
 * the reader, and lives on their membership row as last_read_timestamp.
 */
public record MessageView(

        Long mid,
        Long uid,
        String description,
        Integer media_type,
        Long created_at,
        Long deleted_at,
        Long gid,

        /** True when every other member has opened the conversation since this was sent. */
        boolean is_read
) {

    static MessageView of(Message_ message, long readUpTo) {
        return new MessageView(
                message.mid(),
                message.uid(),
                message.description(),
                message.media_type(),
                message.created_at(),
                message.deleted_at(),
                message.gid(),
                message.created_at() <= readUpTo);
    }
}
