package com.example.World;

import com.example.World.Follows.FollowService;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Searching threads, and not seeing what you should not.
 *
 * The screen filtered the cached feed in memory, so it searched one page of
 * however many threads exist - and an empty result is indistinguishable from no
 * matches, so it looked like it worked rather than like it was broken.
 *
 * Moving it into SQL means the visibility rules are written out a third time.
 * That is the risk here: a search that finds private threads, or a blocked
 * person's, would be a far worse bug than the one being fixed. These are the
 * same cases FeedVisibilityTest runs against the feed, so the two cannot drift
 * without something failing.
 */
@DisplayName("Thread search")
class ThreadSearchTest extends AbstractIntegrationTest {

    // See SecurityRegressionTest for the per-class phone_number blocks in use.
    private static final AtomicLong PHONE = new AtomicLong(2_370_000_000_000L);

    @Autowired ThreadService threads;
    @Autowired ThreadRepository threadRepository;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired FollowService follows;

    @Test
    @DisplayName("finds a thread by title, whatever the case")
    void findsMatches() {
        User_ viewer = user();
        User_ author = user();
        thread(author, "Arsenal to win the league", false);
        thread(author, "Something else entirely", false);

        assertThat(titles(threads.search(viewer.uid(), "ARSENAL")))
                .contains("Arsenal to win the league")
                .doesNotContain("Something else entirely");
    }

    @Test
    @DisplayName("matches part of a title, not only the start")
    void matchesAnywhereInTheTitle() {
        User_ viewer = user();
        thread(user(), "Who wins the derby on Sunday", false);

        assertThat(titles(threads.search(viewer.uid(), "derby")))
                .contains("Who wins the derby on Sunday");
    }

    @Test
    @DisplayName("a private thread needs a mutual follow, same as the feed")
    void respectsPrivateThreads() {
        User_ viewer = user();
        User_ author = user();
        thread(author, "Private prediction about Chelsea", true);

        assertThat(titles(threads.search(viewer.uid(), "Chelsea")))
                .as("a search that finds private threads is a worse bug than a "
                        + "search that finds nothing")
                .isEmpty();

        follows.sendFollow(viewer.uid(), author.uid());
        assertThat(titles(threads.search(viewer.uid(), "Chelsea")))
                .as("one-way is not mutual")
                .isEmpty();

        follows.sendFollow(author.uid(), viewer.uid());
        assertThat(titles(threads.search(viewer.uid(), "Chelsea")))
                .contains("Private prediction about Chelsea");
    }

    @Test
    @DisplayName("a soft-deleted thread is not findable")
    void ignoresDeletedThreads() {
        User_ viewer = user();
        Thread_ removed = thread(user(), "Deleted thread about Liverpool", false);
        // Straight SQL rather than save(): Spring Data JDBC reads a record whose
        // version is still 0 as a new row and tries to insert it again.
        jdbc.update("UPDATE thread_ SET deleted_at = ? WHERE tid = ?",
                new Date().getTime(), removed.tid());

        assertThat(titles(threads.search(viewer.uid(), "Liverpool"))).isEmpty();
    }

    @Test
    @DisplayName("a wildcard in the term is a character, not a wildcard")
    void escapesLikeWildcards() {
        User_ viewer = user();
        thread(user(), "Odds are over 50% on this", false);
        thread(user(), "Nothing to do with odds", false);

        // Unescaped, LIKE '%%%' matches every row - so a search for "50%" would
        // return the whole table and look like the search ignoring the term.
        assertThat(titles(threads.search(viewer.uid(), "%")))
                .as("a bare wildcard must match titles containing a percent sign, "
                        + "not everything")
                .doesNotContain("Nothing to do with odds");

        assertThat(titles(threads.search(viewer.uid(), "50%")))
                .contains("Odds are over 50% on this");
    }

    @Test
    @DisplayName("an empty term returns nothing rather than everything")
    void refusesToListEverything() {
        User_ viewer = user();
        thread(user(), "A thread that exists", false);

        assertThat(threads.search(viewer.uid(), "")).isEmpty();
        assertThat(threads.search(viewer.uid(), "   ")).isEmpty();
        assertThat(threads.search(viewer.uid(), null))
                .as("the screen shows this list only once something is typed; "
                        + "\"no term\" meaning \"everything\" is an unbounded query "
                        + "behind a blank box")
                .isEmpty();
    }

    // --- helpers ----------------------------------------------------------

    private static List<String> titles(List<ThreadProfile> found) {
        return found.stream().map(ThreadProfile::title).toList();
    }

    private Thread_ thread(User_ author, String title, boolean isPrivate) {
        return threadRepository.save(new Thread_(null, author.uid(), title, null, 0,
                "test", 0L, new Date().getTime(), null, isPrivate, null));
    }

    private User_ user() {
        long seq = PHONE.incrementAndGet();
        String name = "search-" + seq;
        return users.save(new User_(null, name, name + "@example.test",
                passwordEncoder.encode("password"),
                "+" + seq, null, true, "", null, new Date().getTime(), null,
                0, null, "offline", null, 0.0, null));
    }
}
