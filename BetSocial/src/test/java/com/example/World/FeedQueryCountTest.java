package com.example.World;

import com.example.World.Comments.CommentRepository;
import com.example.World.Comments.Comment_;
import com.example.World.Threads.ThreadProfile;
import com.example.World.Threads.ThreadRepository;
import com.example.World.Threads.ThreadService;
import com.example.World.Threads.Thread_;
import com.example.World.Users.UserRepository;
import com.example.World.Users.User_;
import com.example.World.support.AbstractIntegrationTest;
import com.example.World.support.QueryCounter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The feed used to issue a fixed number of queries per thread: author lookup,
 * two follow lookups, a like lookup and a full fetch of every comment row just
 * to call .size() on it. Cost therefore grew linearly with the number of
 * threads on screen.
 *
 * This measures actual JDBC statements rather than trusting the shape of the
 * code, and asserts the count no longer depends on how many threads exist.
 */
@Import(QueryCounter.Config.class)
@DisplayName("Feed query count")
class FeedQueryCountTest extends AbstractIntegrationTest {

    private static final AtomicLong PHONE = new AtomicLong(2_346_000_000_000L);

    @Autowired ThreadService threads;
    @Autowired ThreadRepository threadRepository;
    @Autowired CommentRepository comments;
    @Autowired UserRepository users;

    @Test
    @DisplayName("does not grow with the number of threads")
    void feedQueryCountIsConstant() {
        User_ viewer = users.save(user("perf-viewer"));
        User_ author = users.save(user("perf-author"));

        int small = countFeedQueries(viewer, author, 3);
        int large = countFeedQueries(viewer, author, 12);

        System.out.printf("feed queries: 3 threads -> %d, 12 threads -> %d%n", small, large);

        assertThat(large)
                .as("query count must not scale with feed size (was 1 + 5N)")
                .isEqualTo(small);
    }

    /** Seeds `extra` more threads, then counts the statements one feed load issues. */
    private int countFeedQueries(User_ viewer, User_ author, int totalThreads) {
        long existing = threadRepository.findAllActiveThreads().size();
        for (long i = existing; i < totalThreads; i++) {
            Thread_ t = threadRepository.save(new Thread_(
                    null, author.uid(), "perf thread " + i, null, 0,
                    "test", 0L, new Date().getTime(), null, false, null));
            comments.save(new Comment_(null, t.tid(), author.uid(), null,
                    "perf comment", 0L, new Date().getTime(), null, null));
        }

        QueryCounter.reset();
        List<ThreadProfile> feed = threads.threadProfileList(viewer.uid());
        int queries = QueryCounter.count();

        assertThat(feed).as("the feed should actually return threads").isNotEmpty();
        return queries;
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
