package com.example.World;

import com.example.World.Groups.GroupService;
import com.example.World.Groups.Group_;
import com.example.World.Messages.MessageService;
import com.example.World.Messages.Message_;
import com.example.World.Users.UserRepository;
import com.example.World.Users.User_;
import com.example.World.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * sendMessage used to take the recipient straight from the client's STOMP
 * payload. That value is written to the message row and, more importantly,
 * handed to NotificationService, which sends a Firebase push whose body is the
 * message text - so a modified client could name any uid and deliver arbitrary
 * text as a push notification to somebody who was not in the conversation.
 *
 * The recipient is now derived from the sender's own membership row, which no
 * client can influence. The signature no longer accepts one at all, so these
 * tests pin the derivation rather than trying to forge a value.
 */
@DisplayName("Message recipient")
class MessageRecipientTest extends AbstractIntegrationTest {

    // See SecurityRegressionTest for the per-class phone_number blocks in use.
    private static final AtomicLong PHONE = new AtomicLong(2_342_000_000_000L);

    @Autowired MessageService messages;
    @Autowired GroupService groups;
    @Autowired UserRepository users;

    @Test
    @DisplayName("is the conversation's counterparty, not anything the caller names")
    void recipientComesFromMembership() {
        User_ sender = users.save(user());
        User_ peer = users.save(user());
        User_ outsider = users.save(user());

        Group_ dm = groups.createDMGroup(sender.uid() + "" + peer.uid(), sender.uid(), peer.uid());

        Message_ sent = messages.sendMessage(dm.gid(), sender.uid(), "hello", 0);

        assertThat(sent.recipient_id())
                .as("the recipient should be the other member of the conversation")
                .isEqualTo(peer.uid());

        assertThat(sent.recipient_id())
                .as("and never a user who has nothing to do with it")
                .isNotEqualTo(outsider.uid());
    }

    @Test
    @DisplayName("a non-member cannot send into a conversation")
    void nonMemberCannotSend() {
        User_ sender = users.save(user());
        User_ peer = users.save(user());
        User_ outsider = users.save(user());

        Group_ dm = groups.createDMGroup(sender.uid() + "" + peer.uid(), sender.uid(), peer.uid());

        // MessageController already checks membership before calling this, but the
        // service no longer depends on that being done for it: with no membership
        // row there is no recipient to derive, so the send is refused outright.
        assertThatThrownBy(() -> messages.sendMessage(dm.gid(), outsider.uid(), "let me in", 0))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not a member");
    }

    private User_ user() {
        long seq = PHONE.incrementAndGet();
        String name = "recipient-" + seq;
        return new User_(null, name, name + "@example.test",
                "$2a$10$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUV",
                "+" + seq, null, true, "", null, new Date().getTime(), null,
                0, null, "offline", null, 0.0, null);
    }
}
