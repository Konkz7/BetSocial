package com.example.World;

import com.example.World.Groups.GroupService;
import com.example.World.Groups.Group_;
import com.example.World.Messages.MessageRepository;
import com.example.World.Messages.MessageService;
import com.example.World.Messages.Message_;
import com.example.World.Notifications.NotificationRepository;
import com.example.World.Notifications.Notification_;
import com.example.World.Users.UserRepository;
import com.example.World.Users.User_;
import com.example.World.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * sendMessage was built for exactly two people. It resolved a single recipient
 * and dereferenced it, so a group - where there is no one counterparty - threw
 * before a row was ever written and made group chat unreachable however much of
 * the schema was already group-shaped.
 *
 * Delivery is now derived from the conversation's membership rows, which makes a
 * direct message the one-other-person case of the same code path rather than a
 * separate one.
 */
@DisplayName("Group send")
class GroupSendTest extends AbstractIntegrationTest {

    // See SecurityRegressionTest for the per-class phone_number blocks in use.
    private static final AtomicLong PHONE = new AtomicLong(2_343_000_000_000L);

    @Autowired MessageService messages;
    @Autowired MessageRepository messageRepository;
    @Autowired GroupService groups;
    @Autowired UserRepository users;
    @Autowired NotificationRepository notifications;

    @Test
    @DisplayName("a message reaches every member of a three-person group")
    void groupSendReachesEveryMember() {
        User_ creator = users.save(user());
        User_ second = users.save(user());
        User_ third = users.save(user());

        Group_ group = groups.createGroup("the-group", creator.uid(),
                List.of(second.uid(), third.uid()));

        // Previously threw here: recipient_id is null in a group, and the single
        // recipient was resolved with findById(...).orElseThrow().
        Message_ sent = messages.sendMessage(group.gid(), creator.uid(), "hello all", 0);

        assertThat(sent.mid()).as("the message should have been persisted").isNotNull();
        assertThat(messageRepository.findMessagesByGidAsc(group.gid()))
                .as("and should be readable back from the conversation")
                .extracting(Message_::description)
                .containsExactly("hello all");

        assertThat(notificationsFor(second.uid()))
                .as("every other member should be notified")
                .isNotEmpty();
        assertThat(notificationsFor(third.uid()))
                .as("every other member should be notified")
                .isNotEmpty();
        assertThat(notificationsFor(creator.uid()))
                .as("the sender should not be notified of their own message")
                .isEmpty();
    }

    @Test
    @DisplayName("a group message names no single recipient")
    void groupMessageHasNoRecipientId() {
        User_ creator = users.save(user());
        User_ second = users.save(user());
        User_ third = users.save(user());

        Group_ group = groups.createGroup("no-counterparty", creator.uid(),
                List.of(second.uid(), third.uid()));

        Message_ sent = messages.sendMessage(group.gid(), creator.uid(), "hi", 0);

        // recipient_id is direct-message metadata. A group has no one
        // counterparty to name, and nothing derives delivery from it any more.
        assertThat(sent.recipient_id()).isNull();
    }

    @Test
    @DisplayName("a direct message still names its counterparty")
    void directMessageStillCarriesRecipient() {
        User_ sender = users.save(user());
        User_ peer = users.save(user());

        Group_ dm = groups.createDMGroup(sender.uid() + "" + peer.uid(), sender.uid(), peer.uid());

        Message_ sent = messages.sendMessage(dm.gid(), sender.uid(), "just us", 0);

        assertThat(sent.recipient_id())
                .as("a DM is the one-other-person case, not a separate path")
                .isEqualTo(peer.uid());
        assertThat(notificationsFor(peer.uid())).isNotEmpty();
    }

    @Test
    @DisplayName("an offline recipient is still notified")
    void offlineRecipientIsNotified() {
        // The old rule compared the sender's status to the recipient's and skipped
        // the notification when they matched. That is true when both are watching
        // the same chat, but also when both are merely offline - so the recipient
        // who most needed a push was the one who did not get one.
        User_ sender = users.save(user());
        User_ peer = users.save(user());

        assertThat(sender.status()).isEqualTo("offline");
        assertThat(peer.status()).isEqualTo("offline");

        Group_ dm = groups.createDMGroup(sender.uid() + "" + peer.uid(), sender.uid(), peer.uid());
        messages.sendMessage(dm.gid(), sender.uid(), "are you there", 0);

        assertThat(notificationsFor(peer.uid()))
                .as("an offline recipient is exactly who a push is for")
                .isNotEmpty();
    }

    private List<Notification_> notificationsFor(Long uid) {
        return notifications.getActiveNotifications(uid);
    }

    private User_ user() {
        long seq = PHONE.incrementAndGet();
        String name = "groupsend-" + seq;
        return new User_(null, name, name + "@example.test",
                "$2a$10$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUV",
                "+" + seq, null, true, "", null, new Date().getTime(), null,
                0, null, "offline", null, 0.0, null);
    }
}
