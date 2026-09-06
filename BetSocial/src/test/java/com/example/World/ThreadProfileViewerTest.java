package com.example.World;

import com.example.World.Threads.ThreadProfile;
import com.example.World.Threads.ThreadRepository;
import com.example.World.Threads.ThreadService;
import com.example.World.Threads.Thread_;
import com.example.World.Users.UserRepository;
import com.example.World.Users.User_;
import com.example.World.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ThreadService.toThreadProfile reported the "liked" flag for the thread's
 * author rather than for whoever was asking, so the heart on a single thread
 * page showed the author's opinion of their own post. The list variants of the
 * same method already used the viewer.
 */
@DisplayName("Thread profile")
class ThreadProfileViewerTest extends AbstractIntegrationTest {

    private static final AtomicLong PHONE = new AtomicLong(2_347_000_000_000L);

    @Autowired ThreadService threads;
    @Autowired ThreadRepository threadRepository;
    @Autowired UserRepository users;

    @Test
    @DisplayName("reports the liked flag for the viewer, not the author")
    void likedReflectsTheViewer() {
        User_ author = users.save(user("phase5-author"));
        User_ viewer = users.save(user("phase5-viewer"));

        Thread_ thread = threadRepository.save(new Thread_(
                null, author.uid(), "phase5 viewer thread", null, 0,
                "test", 0L, new Date().getTime(), null, false, null));

        // The viewer likes it; the author does not.
        threads.registerThreadLike(viewer.uid(), thread.tid(), true);

        ThreadProfile asViewer = threads.toThreadProfile(thread.tid(), viewer.uid());
        ThreadProfile asAuthor = threads.toThreadProfile(thread.tid(), author.uid());

        assertThat(asViewer.liked())
                .as("the viewer liked this thread")
                .isTrue();
        assertThat(asAuthor.liked())
                .as("the author did not like their own thread")
                .isFalse();
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
