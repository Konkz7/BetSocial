package com.example.World;

import com.example.World.Notifications.NotificationDTO;
import com.example.World.Notifications.NotificationRepository;
import com.example.World.Notifications.NotificationService;
import com.example.World.Users.UserRepository;
import com.example.World.Users.User_;
import com.example.World.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What the Notifications toggle actually turns off.
 *
 * It is push, and only push. The rows behind the activity list are written
 * either way: switching this off is asking the phone to stay quiet, not asking
 * to stop being notified, and an activity screen that emptied itself would be
 * answering a different question than the one the toggle asks.
 *
 * That distinction is the whole feature, and it is invisible from the toggle -
 * which is why it is pinned here rather than left to whoever reads the service
 * next.
 */
@DisplayName("Notification preference")
class NotificationPreferenceTest extends AbstractIntegrationTest {

    // See SecurityRegressionTest for the per-class phone_number blocks in use.
    private static final AtomicLong PHONE = new AtomicLong(2_375_000_000_000L);

    @Autowired NotificationService notifications;
    @Autowired NotificationRepository notificationRepository;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("everybody starts with it on")
    void defaultsToOn() {
        assertThat(users.pushEnabled(user().uid()))
                .as("the column defaults to true, because that is what every "
                        + "existing account already gets - a default of false "
                        + "would switch push off for everyone on deploy")
                .isTrue();
    }

    @Test
    @DisplayName("turning it off still writes the activity row")
    void keepsTheActivityList() {
        User_ recipient = user();
        User_ actor = user();
        users.setPushEnabled(recipient.uid(), false);

        notifications.registerNotification("a-token", "liked your thread",
                new NotificationDTO(actor.uid(), "thread_like", 1L, "thread"),
                recipient.uid());

        assertThat(notificationRepository.findAll().stream()
                .filter(n -> n.uid().equals(recipient.uid()))
                .toList())
                .as("switching push off asks the phone to stay quiet; it does not "
                        + "ask to stop being notified, and this list is where that "
                        + "notification still lives")
                .isNotEmpty();
    }

    @Test
    @DisplayName("the setting sticks, both ways")
    void persistsBothWays() {
        User_ person = user();

        users.setPushEnabled(person.uid(), false);
        assertThat(users.pushEnabled(person.uid())).isFalse();

        users.setPushEnabled(person.uid(), true);
        assertThat(users.pushEnabled(person.uid())).isTrue();
    }

    @Test
    @DisplayName("a conversation asks once, not once per recipient")
    void filtersARoomInOneQuery() {
        User_ wants = user();
        User_ doesNot = user();
        users.setPushEnabled(doesNot.uid(), false);

        List<Long> allowed = users.withPushEnabled(List.of(wants.uid(), doesNot.uid()));

        assertThat(allowed)
                .as("a conversation push already had to stop being one call per "
                        + "member once; a filter per recipient is the same mistake")
                .containsExactly(wants.uid());
    }

    @Test
    @DisplayName("nobody in the room wants push")
    void handlesEverybodyOff() {
        User_ first = user();
        User_ second = user();
        users.setPushEnabled(first.uid(), false);
        users.setPushEnabled(second.uid(), false);

        assertThat(users.withPushEnabled(List.of(first.uid(), second.uid())))
                .as("an empty list has to be an empty list, not a query that "
                        + "falls back to everybody")
                .isEmpty();
    }

    // --- helpers ----------------------------------------------------------

    private User_ user() {
        long seq = PHONE.incrementAndGet();
        String name = "push-" + seq;
        return users.save(new User_(null, name, name + "@example.test",
                passwordEncoder.encode("password"),
                "+" + seq, null, true, "", null, new Date().getTime(), null,
                0, null, "offline", null, 0.0, null));
    }
}
