package com.example.World;

import com.example.World.Groups.GroupService;
import com.example.World.Groups.Group_;
import com.example.World.Messages.MessageService;
import com.example.World.Notifications.NotificationRepository;
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
 * Who receives a message is now read from the conversation's membership rows,
 * which no client can influence. Neither the method signature nor the message row
 * has a recipient on it any more, so there is no value left to forge - these pin
 * the audience itself instead.
 */
@DisplayName("Message recipient")
class MessageRecipientTest extends AbstractIntegrationTest {

    // See SecurityRegressionTest for the per-class phone_number blocks in use.
    private static final AtomicLong PHONE = new AtomicLong(2_342_000_000_000L);

    @Autowired MessageService messages;
    @Autowired GroupService groups;
    @Autowired UserRepository users;
    @Autowired NotificationRepository notifications;

    @Test
    @DisplayName("a message reaches its conversation and nobody beyond it")
    void audienceIsTheConversation() {
        User_ sender = users.save(user());
        User_ peer = users.save(user());
        User_ outsider = users.save(user());

        Group_ direct = groups.openDirectConversation(sender.uid(), peer.uid());

        messages.sendMessage(direct.gid(), sender.uid(), "hello", 0);

        assertThat(notifications.getActiveNotifications(peer.uid()))
                .as("the other member of the conversation is notified")
                .isNotEmpty();

        // The abuse this closes: registerNotification sends a push whose body is
        // the message text, so naming an arbitrary uid delivered arbitrary text to
        // a stranger. There is no longer any way to name one.
        assertThat(notifications.getActiveNotifications(outsider.uid()))
                .as("somebody outside it must not be reachable through it")
                .isEmpty();
    }

    @Test
    @DisplayName("a non-member cannot send into a conversation")
    void nonMemberCannotSend() {
        User_ sender = users.save(user());
        User_ peer = users.save(user());
        User_ outsider = users.save(user());

        Group_ dm = groups.openDirectConversation(sender.uid(), peer.uid());

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
