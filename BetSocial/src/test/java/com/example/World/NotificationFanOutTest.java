package com.example.World;

import com.example.World.Groups.GroupService;
import com.example.World.Groups.Group_;
import com.example.World.Messages.MessageService;
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
 * Sending to a conversation notified its members one at a time, and each of those
 * calls re-read the sender, re-read the recipient the caller was already holding,
 * and made its own blocking call to Firebase - all of it before the message was
 * broadcast to the topic, so a large group made everybody's live delivery wait on
 * a round trip to Google per member.
 *
 * The push is one multicast now, which is possible because every recipient of one
 * message gets the same text. What remains per recipient is their own notification
 * row and its de-duplication, which is database work rather than the network.
 */
@DisplayName("Notification fan-out")
class NotificationFanOutTest extends AbstractIntegrationTest {

    // See SecurityRegressionTest for the per-class phone_number blocks in use.
    private static final AtomicLong PHONE = new AtomicLong(2_351_000_000_000L);

    @Autowired MessageService messages;
    @Autowired GroupService groups;
    @Autowired UserRepository users;
    @Autowired NotificationRepository notifications;

    @Test
    @DisplayName("names the group and says who spoke")
    void groupWordingNamesTheGroup() {
        User_ sender = users.save(user("chris"));
        User_ first = users.save(user("emily"));
        User_ second = users.save(user("dan"));

        Group_ group = groups.createGroup("Fantasy League", sender.uid(),
                List.of(first.uid(), second.uid()));

        messages.sendMessage(group.gid(), sender.uid(), "anyone free saturday", 0);

        // A group notification that said only "you have a message from chris" gave
        // no way to tell which conversation it came from.
        for (User_ member : List.of(first, second)) {
            Notification_ notification = onlyNotificationFor(member);
            assertThat(notification.title()).isEqualTo("Fantasy League");
            assertThat(notification.body()).isEqualTo(sender.user_name() + ": anyone free saturday");
        }

        assertThat(notifications.getActiveNotifications(sender.uid()))
                .as("the sender is not told about their own message")
                .isEmpty();
    }

    @Test
    @DisplayName("names the person in a direct conversation")
    void directWordingNamesTheSender() {
        User_ sender = users.save(user("chris"));
        User_ peer = users.save(user("emily"));

        Group_ direct = groups.openDirectConversation(sender.uid(), peer.uid());

        messages.sendMessage(direct.gid(), sender.uid(), "you around", 0);

        // There is no group to name, so the sender is the title and the message
        // stands alone - rather than opening with the recipient's own name, which
        // told them who they are and buried who it was from.
        Notification_ notification = onlyNotificationFor(peer);
        assertThat(notification.title()).isEqualTo(sender.user_name());
        assertThat(notification.body()).isEqualTo("you around");
    }

    @Test
    @DisplayName("collapses a burst of messages into one notification per member")
    void repeatedMessagesCollapse() {
        User_ sender = users.save(user("chris"));
        User_ first = users.save(user("emily"));
        User_ second = users.save(user("dan"));

        Group_ group = groups.createGroup("Fantasy League", sender.uid(),
                List.of(first.uid(), second.uid()));

        for (int i = 1; i <= 20; i++) {
            messages.sendMessage(group.gid(), sender.uid(), "message " + i, 0);
        }

        // Twenty messages, one notification each - not twenty apiece. This is what
        // keeps a busy group from burying everything else in the activity list.
        for (User_ member : List.of(first, second)) {
            Notification_ notification = onlyNotificationFor(member);
            assertThat(notification.body())
                    .as("and it carries the most recent message")
                    .isEqualTo(sender.user_name() + ": message 20");
        }
    }

    @Test
    @DisplayName("still records notifications when nobody has a device registered")
    void worksWithoutPushTokens() {
        User_ sender = users.save(user("chris"));
        User_ peer = users.save(user("emily"));

        // fb_notification_token is null for everyone here. Passing null through used
        // to raise an exception per recipient, caught and logged one at a time.
        assertThat(peer.fb_notification_token()).isNull();

        Group_ direct = groups.openDirectConversation(sender.uid(), peer.uid());
        messages.sendMessage(direct.gid(), sender.uid(), "no device", 0);

        assertThat(onlyNotificationFor(peer).body())
                .as("the in-app notification does not depend on push being possible")
                .isEqualTo("no device");
    }

    private Notification_ onlyNotificationFor(User_ user) {
        List<Notification_> found = notifications.getActiveNotifications(user.uid());
        assertThat(found).as("exactly one notification for %s", user.user_name()).hasSize(1);
        return found.get(0);
    }

    private User_ user(String name) {
        long seq = PHONE.incrementAndGet();
        String unique = name + "-" + seq;
        return new User_(null, unique, unique + "@example.test",
                "$2a$10$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUV",
                "+" + seq, null, true, "", null, new Date().getTime(), null,
                0, null, "offline", null, 0.0, null);
    }
}
