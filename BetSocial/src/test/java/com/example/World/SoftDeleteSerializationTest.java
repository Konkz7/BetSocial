package com.example.World;

import com.example.World.Bets.BetRepository;
import com.example.World.Bets.Bet_;
import com.example.World.Bets.Status;
import com.example.World.Comments.CommentRepository;
import com.example.World.Comments.Comment_;
import com.example.World.Threads.ThreadRepository;
import com.example.World.Threads.ThreadService;
import com.example.World.Threads.Thread_;
import com.example.World.Users.UserRepository;
import com.example.World.Users.User_;
import com.example.World.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * The entities overrode created_at() to throw IllegalStateException on
 * combinations of timestamps they considered invalid. Because an accessor runs
 * when Jackson serialises the record and when Spring Data maps it, a row that
 * trips one of those checks becomes permanently unreadable - a 500 on every
 * request that touches it, with no way to correct it through the API.
 *
 * Bet_ was the worst case. It threw when deleted_at <= ends_at, which is the
 * normal outcome of deleting a thread: removeThread cancels and soft-deletes
 * its still-active bets, whose ends_at is in the future.
 */
@DisplayName("Soft-deleted rows")
class SoftDeleteSerializationTest extends AbstractIntegrationTest {

    private static final AtomicLong PHONE = new AtomicLong(2_344_000_000_000L);
    private final ObjectMapper json = new ObjectMapper();

    @Autowired ThreadService threadService;
    @Autowired ThreadRepository threadRepository;
    @Autowired BetRepository bets;
    @Autowired CommentRepository comments;
    @Autowired UserRepository users;

    @Test
    @DisplayName("a bet soft-deleted before it ends is still serializable")
    void deletedActiveBetIsSerializable() {
        User_ author = users.save(user("acc-a"));
        long now = new Date().getTime();
        Thread_ thread = threadRepository.save(thread(author, now));

        Bet_ bet = bets.save(new Bet_(null, thread.tid(), Status.ACTIVE.toInt(), null, 0L, 0L,
                "accessor bet", now, null, now + 86_400_000L,
                false, false, false, 100L, 1L, null));

        // Exactly what removeThread does to a live bet.
        threadService.removeThread(thread.tid(), author.uid());

        Bet_ reloaded = bets.findById(bet.bid()).orElseThrow();
        assertThat(reloaded.deleted_at()).isNotNull();
        assertThatCode(() -> json.writeValueAsString(reloaded))
                .as("deleting a thread must not make its bets unreadable")
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("a row deleted in the same millisecond it was created is still serializable")
    void sameMillisecondDeleteIsSerializable() {
        User_ author = users.save(user("acc-b"));
        long now = new Date().getTime();
        Thread_ thread = threadRepository.save(thread(author, now));

        Comment_ comment = comments.save(new Comment_(null, thread.tid(), author.uid(), null,
                "accessor comment", 0L, now, null, null));
        comments.softDelete(comment.cid(), now);   // deleted_at == created_at

        Comment_ reloaded = comments.findById(comment.cid()).orElseThrow();
        assertThatCode(() -> json.writeValueAsString(reloaded))
                .as("a same-millisecond delete must not poison the row")
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("the feed still loads after a thread is deleted")
    void feedSurvivesDeletedThread() {
        User_ author = users.save(user("acc-c"));
        long now = new Date().getTime();
        Thread_ thread = threadRepository.save(thread(author, now));
        bets.save(new Bet_(null, thread.tid(), Status.ACTIVE.toInt(), null, 0L, 0L,
                "accessor bet 2", now, null, now + 86_400_000L,
                false, false, false, 100L, 1L, null));

        threadService.removeThread(thread.tid(), author.uid());

        assertThatCode(() -> json.writeValueAsString(bets.findAll()))
                .as("listing bets must not blow up because one was soft-deleted")
                .doesNotThrowAnyException();
    }

    private Thread_ thread(User_ author, long now) {
        return new Thread_(null, author.uid(), "accessor thread", null, 0,
                "test", 0L, now, null, false, null);
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
