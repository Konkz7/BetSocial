package com.example.World;

import com.example.World.Comments.CommentRepository;
import com.example.World.Comments.CommentService;
import com.example.World.Comments.Comment_;
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

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Behavioural guard for the batched feed assembly.
 *
 * Batching the feed's lookups is only worth doing if the result is identical, so
 * this pins the rules the old per-thread loop enforced: private threads need a
 * mutual follow, an author always sees their own, and the liked flag and comment
 * count are per-viewer and per-thread.
 */
@DisplayName("Feed visibility")
class FeedVisibilityTest extends AbstractIntegrationTest {

    private static final AtomicLong PHONE = new AtomicLong(2_345_000_000_000L);

    @Autowired ThreadService threads;
    @Autowired ThreadRepository threadRepository;
    @Autowired CommentRepository comments;
    @Autowired CommentService commentService;
    @Autowired FollowService follows;
    @Autowired UserRepository users;

    @Test
    @DisplayName("shows public threads to everyone")
    void publicThreadsAreVisible() {
        User_ author = users.save(user("vis-a1"));
        User_ stranger = users.save(user("vis-a2"));
        Thread_ t = thread(author, "public thread", false);

        assertThat(tids(threads.threadProfileList(stranger.uid()))).contains(t.tid());
    }

    @Test
    @DisplayName("hides private threads without a mutual follow")
    void privateThreadsHiddenFromStrangers() {
        User_ author = users.save(user("vis-b1"));
        User_ stranger = users.save(user("vis-b2"));
        Thread_ t = thread(author, "private thread", true);

        assertThat(tids(threads.threadProfileList(stranger.uid()))).doesNotContain(t.tid());
    }

    @Test
    @DisplayName("hides private threads when the follow is only one-way")
    void privateThreadsNeedMutualFollow() {
        User_ author = users.save(user("vis-c1"));
        User_ viewer = users.save(user("vis-c2"));
        Thread_ t = thread(author, "private thread", true);

        follows.sendFollow(viewer.uid(), author.uid());   // one direction only

        assertThat(tids(threads.threadProfileList(viewer.uid()))).doesNotContain(t.tid());

        follows.sendFollow(author.uid(), viewer.uid());   // now mutual

        assertThat(tids(threads.threadProfileList(viewer.uid()))).contains(t.tid());
    }

    @Test
    @DisplayName("always shows an author their own private thread")
    void authorSeesOwnPrivateThread() {
        User_ author = users.save(user("vis-d1"));
        Thread_ t = thread(author, "private thread", true);

        assertThat(tids(threads.threadProfileList(author.uid()))).contains(t.tid());
    }

    @Test
    @DisplayName("reports the liked flag and comment count per thread")
    void likedFlagAndCommentCount() {
        User_ author = users.save(user("vis-e1"));
        User_ viewer = users.save(user("vis-e2"));
        Thread_ liked = thread(author, "liked thread", false);
        Thread_ plain = thread(author, "plain thread", false);

        threads.registerThreadLike(viewer.uid(), liked.tid(), true);
        comments.save(comment(liked, author));
        comments.save(comment(liked, author));

        List<ThreadProfile> feed = threads.threadProfileList(viewer.uid());

        assertThat(profile(feed, liked.tid()).liked()).isTrue();
        assertThat(profile(feed, liked.tid()).commentCount()).isEqualTo(2L);
        assertThat(profile(feed, plain.tid()).liked()).isFalse();
        assertThat(profile(feed, plain.tid()).commentCount())
                .as("a thread with no comments counts zero, not null")
                .isEqualTo(0L);
    }

    @Test
    @DisplayName("counts soft-deleted comments, as the previous implementation did")
    void commentCountIncludesSoftDeleted() {
        User_ author = users.save(user("vis-f1"));
        Thread_ t = thread(author, "thread with a deleted comment", false);

        Comment_ kept = comments.save(comment(t, author));
        Comment_ removed = comments.save(comment(t, author));
        commentService.deleteComment(removed.cid());

        assertThat(profile(threads.threadProfileList(author.uid()), t.tid()).commentCount())
                .as("findByThread never filtered deleted_at, so the count must not either")
                .isEqualTo(2L);
    }

    // --- helpers ---

    private static List<Long> tids(List<ThreadProfile> feed) {
        return feed.stream().map(ThreadProfile::tid).toList();
    }

    private static ThreadProfile profile(List<ThreadProfile> feed, Long tid) {
        Optional<ThreadProfile> found = feed.stream().filter(p -> p.tid().equals(tid)).findFirst();
        assertThat(found).as("thread %s should be in the feed", tid).isPresent();
        return found.get();
    }

    private Thread_ thread(User_ author, String title, boolean isPrivate) {
        return threadRepository.save(new Thread_(null, author.uid(), title, null, 0,
                "test", 0L, new Date().getTime(), null, isPrivate, null));
    }

    private static Comment_ comment(Thread_ t, User_ author) {
        return new Comment_(null, t.tid(), author.uid(), null, "a comment",
                0L, new Date().getTime(), null, null);
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
