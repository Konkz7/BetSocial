package com.example.World;

import com.example.World.Blocks.BlockService;
import com.example.World.Threads.FeedPage;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Paging the feed.
 *
 * The feed returned every thread in the database on every app launch. It also
 * decided visibility in Java after the query, which is why the rules moved into
 * SQL as part of this: filtering after a LIMIT hands back fewer rows than the
 * page asked for, and can return an empty page while more threads exist - which
 * a client cannot tell apart from the end of the feed. Threads would simply go
 * missing.
 *
 * Assertions here are deliberately relative rather than absolute counts: the
 * container is shared with every other test class, so the feed already holds
 * threads this class did not create.
 */
@DisplayName("Feed pagination")
class FeedPaginationTest extends AbstractIntegrationTest {

    // See SecurityRegressionTest for the per-class phone_number blocks in use.
    private static final AtomicLong PHONE = new AtomicLong(2_358_000_000_000L);

    private static final int PAGE_SIZE = 20;

    @Autowired ThreadService threads;
    @Autowired ThreadRepository threadRepository;
    @Autowired UserRepository users;
    @Autowired BlockService blockService;

    @Test
    @DisplayName("a page is never bigger than the page size")
    void pageIsBounded() {
        User_ author = user();
        User_ viewer = user();
        long now = new Date().getTime();
        for (int i = 0; i < PAGE_SIZE + 15; i++) {
            thread(author, "bounded " + i, now + i);
        }

        FeedPage first = threads.feedPage(viewer.uid(), null, null);

        assertThat(first.threads()).hasSizeLessThanOrEqualTo(PAGE_SIZE);
        assertThat(first.has_more())
                .as("there are more than one page of threads in the database")
                .isTrue();
    }

    @Test
    @DisplayName("a page with more after it is full - filtering never shortens it")
    void aPageWithMoreAfterItIsFull() {
        User_ author = user();
        User_ viewer = user();
        long now = new Date().getTime();
        for (int i = 0; i < PAGE_SIZE * 2; i++) {
            thread(author, "full " + i, now + i);
        }

        FeedPage page = threads.feedPage(viewer.uid(), null, null);

        // This is the property that broke when visibility was decided in Java:
        // a page could come back short, or empty, with more still to come.
        assertThat(page.has_more()).isTrue();
        assertThat(page.threads())
                .as("if there is more after this page, this page must be full")
                .hasSize(PAGE_SIZE);
    }

    @Test
    @DisplayName("walking the cursor reaches every thread exactly once")
    void cursorWalkIsCompleteAndHasNoDuplicates() {
        User_ author = user();
        User_ viewer = user();
        long now = new Date().getTime();
        Set<Long> created = new HashSet<>();
        for (int i = 0; i < PAGE_SIZE * 2 + 7; i++) {
            created.add(thread(author, "walk " + i, now + i).tid());
        }

        List<ThreadProfile> seen = walk(viewer.uid());
        List<Long> tids = seen.stream().map(ThreadProfile::tid).toList();

        assertThat(tids).doesNotHaveDuplicates();
        assertThat(tids).containsAll(created);
    }

    @Test
    @DisplayName("threads sharing a timestamp are not skipped")
    void identicalTimestampsAreNotSkipped() {
        User_ author = user();
        User_ viewer = user();

        // Every one of these has the same created_at. A cursor on created_at
        // alone would step straight past the rest of the group once it had seen
        // one of them, losing most of this set. The tid is in the cursor for
        // exactly this reason - and a feed is the one place where a burst of
        // posts in the same millisecond is normal rather than contrived.
        long sameMoment = new Date().getTime();
        Set<Long> created = new HashSet<>();
        for (int i = 0; i < PAGE_SIZE + 12; i++) {
            created.add(thread(author, "same moment " + i, sameMoment).tid());
        }

        List<Long> tids = walk(viewer.uid()).stream().map(ThreadProfile::tid).toList();

        assertThat(tids).doesNotHaveDuplicates();
        assertThat(tids)
                .as("a created_at-only cursor loses all but the first of these")
                .containsAll(created);
    }

    @Test
    @DisplayName("the last page says so and carries no cursor")
    void lastPageHasNoCursor() {
        User_ viewer = user();

        FeedPage last = null;
        Long cursorCreatedAt = null;
        Long cursorTid = null;
        for (int page = 0; page < 200; page++) {
            last = threads.feedPage(viewer.uid(), cursorCreatedAt, cursorTid);
            if (!last.has_more()) {
                break;
            }
            cursorCreatedAt = last.next_cursor_created_at();
            cursorTid = last.next_cursor_tid();
        }

        assertThat(last).isNotNull();
        assertThat(last.has_more()).isFalse();
        assertThat(last.next_cursor_created_at()).isNull();
        assertThat(last.next_cursor_tid()).isNull();
    }

    @Test
    @DisplayName("newest first")
    void newestFirst() {
        User_ author = user();
        User_ viewer = user();
        long now = new Date().getTime();
        Thread_ older = thread(author, "older", now);
        Thread_ newer = thread(author, "newer", now + 1_000);

        List<Long> tids = threads.feedPage(viewer.uid(), null, null)
                .threads().stream().map(ThreadProfile::tid).toList();

        assertThat(tids.indexOf(newer.tid()))
                .as("both are on the first page, newest first")
                .isLessThan(tids.indexOf(older.tid()));
    }

    @Test
    @DisplayName("half a cursor is refused rather than silently restarting")
    void halfACursorIsRefused() {
        User_ viewer = user();

        assertThatThrownBy(() -> threads.feedPage(viewer.uid(), new Date().getTime(), null))
                .as("silently treating it as the first page would loop a client forever")
                .isInstanceOf(ResponseStatusException.class);

        assertThatThrownBy(() -> threads.feedPage(viewer.uid(), null, 1L))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("a blocked author is excluded by the query, not after it")
    void blockedAuthorNeverEntersThePage() {
        User_ nuisance = user();
        User_ viewer = user();
        long now = new Date().getTime();
        Set<Long> theirs = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            theirs.add(thread(nuisance, "blocked author " + i, now + i).tid());
        }

        blockService.block(viewer.uid(), nuisance.uid());

        List<Long> tids = walk(viewer.uid()).stream().map(ThreadProfile::tid).toList();

        assertThat(tids).doesNotContainAnyElementsOf(theirs);
    }

    // --- helpers ----------------------------------------------------------

    private List<ThreadProfile> walk(Long viewerUid) {
        List<ThreadProfile> all = new ArrayList<>();
        Long cursorCreatedAt = null;
        Long cursorTid = null;

        for (int page = 0; page < 200; page++) {
            FeedPage current = threads.feedPage(viewerUid, cursorCreatedAt, cursorTid);
            all.addAll(current.threads());
            if (!current.has_more()) {
                return all;
            }
            cursorCreatedAt = current.next_cursor_created_at();
            cursorTid = current.next_cursor_tid();
        }
        throw new AssertionError("the feed cursor never reached the end - it is not advancing");
    }

    private Thread_ thread(User_ author, String title, long createdAt) {
        return threadRepository.save(new Thread_(null, author.uid(), title, null, 0,
                "test", 0L, createdAt, null, false, null));
    }

    private User_ user() {
        long seq = PHONE.incrementAndGet();
        String name = "paging-" + seq;
        return users.save(new User_(null, name, name + "@example.test",
                "$2a$10$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUV",
                "+" + seq, null, true, "", null, new Date().getTime(), null,
                0, null, "offline", null, 0.0, null));
    }
}
