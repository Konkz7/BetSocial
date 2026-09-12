package com.example.World;

import com.example.World.Groups.GroupService;
import com.example.World.Groups.Group_;
import com.example.World.Messages.MessagePage;
import com.example.World.Messages.MessageRepository;
import com.example.World.Messages.MessageService;
import com.example.World.Messages.MessageView;
import com.example.World.Messages.Message_;
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
 * Paging a conversation.
 *
 * A conversation returned its entire history on every open, rendered by a
 * ScrollView that mounts every message at once. Unbounded in principle, and the
 * cost falls hardest on the longest-running conversations - the ones people care
 * about most.
 *
 * Pages come back newest first, because that is the end a chat is opened at.
 * Paging forwards from the oldest message would mean fetching a whole history to
 * show the message somebody just received.
 */
@DisplayName("Message pagination")
class MessagePaginationTest extends AbstractIntegrationTest {

    // See SecurityRegressionTest for the per-class phone_number blocks in use.
    private static final AtomicLong PHONE = new AtomicLong(2_359_000_000_000L);

    private static final int PAGE_SIZE = 40;

    @Autowired MessageService messages;
    @Autowired MessageRepository messageRepository;
    @Autowired GroupService groups;
    @Autowired UserRepository users;

    @Test
    @DisplayName("opens at the newest messages")
    void opensAtTheNewest() {
        User_ owner = user();
        Group_ chat = chat(owner);
        long now = new Date().getTime();
        for (int i = 0; i < PAGE_SIZE + 10; i++) {
            message(chat, owner, "message " + i, now + i);
        }

        MessagePage page = messages.messagePage(chat.gid(), owner.uid(), null, null);

        assertThat(page.messages()).hasSize(PAGE_SIZE);
        assertThat(page.messages().get(0).description())
                .as("newest first, so the client's inverted list renders it at the bottom")
                .isEqualTo("message " + (PAGE_SIZE + 9));
        assertThat(page.has_more()).isTrue();
    }

    @Test
    @DisplayName("a page with more before it is full")
    void aPageWithMoreBeforeItIsFull() {
        User_ owner = user();
        Group_ chat = chat(owner);
        long now = new Date().getTime();
        for (int i = 0; i < PAGE_SIZE * 2; i++) {
            message(chat, owner, "full " + i, now + i);
        }

        MessagePage page = messages.messagePage(chat.gid(), owner.uid(), null, null);

        assertThat(page.has_more()).isTrue();
        assertThat(page.messages()).hasSize(PAGE_SIZE);
    }

    @Test
    @DisplayName("walking back reaches every message exactly once")
    void walkingBackIsCompleteAndHasNoDuplicates() {
        User_ owner = user();
        Group_ chat = chat(owner);
        long now = new Date().getTime();
        Set<Long> created = new HashSet<>();
        for (int i = 0; i < PAGE_SIZE * 2 + 13; i++) {
            created.add(message(chat, owner, "walk " + i, now + i).mid());
        }

        List<Long> mids = walk(chat.gid(), owner.uid()).stream()
                .map(MessageView::mid).toList();

        assertThat(mids).doesNotHaveDuplicates();
        assertThat(mids).containsAll(created);
    }

    @Test
    @DisplayName("messages sharing a timestamp are not skipped")
    void identicalTimestampsAreNotSkipped() {
        User_ owner = user();
        Group_ chat = chat(owner);

        // Two people replying at once, or one client sending a burst, land in the
        // same millisecond regularly - a conversation is where a created_at-only
        // cursor stops being a theoretical worry and starts losing messages.
        long sameMoment = new Date().getTime();
        Set<Long> created = new HashSet<>();
        for (int i = 0; i < PAGE_SIZE + 15; i++) {
            created.add(message(chat, owner, "same moment " + i, sameMoment).mid());
        }

        List<Long> mids = walk(chat.gid(), owner.uid()).stream()
                .map(MessageView::mid).toList();

        assertThat(mids).doesNotHaveDuplicates();
        assertThat(mids)
                .as("a created_at-only cursor loses all but one of these")
                .containsAll(created);
    }

    @Test
    @DisplayName("the oldest page says so and carries no cursor")
    void oldestPageHasNoCursor() {
        User_ owner = user();
        Group_ chat = chat(owner);
        long now = new Date().getTime();
        for (int i = 0; i < 5; i++) {
            message(chat, owner, "short history " + i, now + i);
        }

        MessagePage page = messages.messagePage(chat.gid(), owner.uid(), null, null);

        assertThat(page.has_more()).isFalse();
        assertThat(page.next_cursor_created_at()).isNull();
        assertThat(page.next_cursor_mid()).isNull();
    }

    @Test
    @DisplayName("an empty conversation is an empty page, not an error")
    void emptyConversation() {
        User_ owner = user();
        Group_ chat = chat(owner);

        MessagePage page = messages.messagePage(chat.gid(), owner.uid(), null, null);

        assertThat(page.messages()).isEmpty();
        assertThat(page.has_more()).isFalse();
    }

    @Test
    @DisplayName("a non-member is refused before the cursor is even read")
    void nonMemberIsRefused() {
        User_ owner = user();
        Group_ chat = chat(owner);
        User_ outsider = user();

        assertThatThrownBy(() -> messages.messagePage(chat.gid(), outsider.uid(), null, null))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("half a cursor is refused rather than silently reopening at the end")
    void halfACursorIsRefused() {
        User_ owner = user();
        Group_ chat = chat(owner);

        assertThatThrownBy(() ->
                messages.messagePage(chat.gid(), owner.uid(), new Date().getTime(), null))
                .as("silently returning the newest page would loop a client scrolling back")
                .isInstanceOf(ResponseStatusException.class);

        assertThatThrownBy(() -> messages.messagePage(chat.gid(), owner.uid(), null, 1L))
                .isInstanceOf(ResponseStatusException.class);
    }

    // --- helpers ----------------------------------------------------------

    private List<MessageView> walk(Long gid, Long uid) {
        List<MessageView> all = new ArrayList<>();
        Long cursorCreatedAt = null;
        Long cursorMid = null;

        for (int page = 0; page < 200; page++) {
            MessagePage current = messages.messagePage(gid, uid, cursorCreatedAt, cursorMid);
            all.addAll(current.messages());
            if (!current.has_more()) {
                return all;
            }
            cursorCreatedAt = current.next_cursor_created_at();
            cursorMid = current.next_cursor_mid();
        }
        throw new AssertionError("the message cursor never reached the start - it is not advancing");
    }

    private Group_ chat(User_ owner) {
        User_ other = user();
        return groups.createGroup(null, owner.uid(), List.of(other.uid()));
    }

    private Message_ message(Group_ chat, User_ sender, String text, long createdAt) {
        return messageRepository.save(new Message_(null, sender.uid(), text, 0,
                createdAt, null, chat.gid(), null));
    }

    private User_ user() {
        long seq = PHONE.incrementAndGet();
        String name = "msgpage-" + seq;
        return users.save(new User_(null, name, name + "@example.test",
                "$2a$10$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUV",
                "+" + seq, null, true, "", null, new Date().getTime(), null,
                0, null, "offline", null, 0.0, null));
    }
}
