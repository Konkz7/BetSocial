package com.example.World;

import com.example.World.Notifications.NotificationRepository;
import com.example.World.Notifications.Notification_;
import com.example.World.Users.UserRepository;
import com.example.World.Users.User_;
import com.example.World.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * findLatestNonDeleted returns an Optional and orders by created_at DESC, but
 * had no LIMIT. Postgres returns every match, and Spring Data then throws
 * IncorrectResultSizeDataAccessException as soon as a second row qualifies - so
 * the de-duplication path in NotificationService fails exactly when it is
 * needed, on the second identical notification.
 */
@DisplayName("Notification de-duplication")
class NotificationDedupeTest extends AbstractIntegrationTest {

    private static final AtomicLong PHONE = new AtomicLong(2_348_000_000_000L);

    @Autowired NotificationRepository notifications;
    @Autowired UserRepository users;

    @Test
    @DisplayName("returns the most recent match when several exist")
    void returnsLatestRatherThanThrowing() {
        User_ recipient = users.save(user("phase5-recipient"));
        User_ actor = users.save(user("phase5-actor"));
        long now = System.currentTimeMillis();

        // Identical on every column the lookup filters by; only the body differs.
        notifications.save(notification(recipient, actor, now, "older body"));
        notifications.save(notification(recipient, actor, now + 1_000, "newer body"));

        Optional<Notification_> found = notifications.findLatestNonDeleted(
                "thread_like", recipient.uid(), actor.uid(), 993L);

        assertThat(found).as("two matching rows must not blow up the lookup").isPresent();
        assertThat(found.get().body())
                .as("ORDER BY created_at DESC means the newest row wins")
                .isEqualTo("newer body");
    }

    private static Notification_ notification(User_ recipient, User_ actor, long createdAt, String body) {
        return new Notification_(
                null, recipient.uid(), actor.uid(), "thread_like", 993L, "thread",
                "title", body, false, false, createdAt);
    }

    private User_ user(String name) {
        long now = new Date().getTime();
        String unique = name + "-" + PHONE.get();
        return new User_(null, unique, unique + "@example.test",
                "$2a$10$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUV",
                "+" + PHONE.incrementAndGet(), null, true, "", null, now, null,
                0, null, "offline", null, 0.0, null);
    }
}
