package com.example.World;

import com.example.World.Groups.GroupService;
import com.example.World.Groups.Group_;
import com.example.World.Messages.ConversationDTO;
import com.example.World.Messages.MessageService;
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
 * MessageService.getConversations builds its result by zipping two separately
 * queried lists together by list position, and dereferences group.last_mid
 * without checking it.
 *
 * A conversation that exists but has no messages yet - which is exactly what
 * opening a direct conversation produces - therefore breaks the caller's entire conversation
 * list, not just that one row.
 */
@DisplayName("Conversation list")
class ConversationListTest extends AbstractIntegrationTest {

    private static final AtomicLong PHONE = new AtomicLong(2_349_000_000_000L);

    @Autowired MessageService messages;
    @Autowired GroupService groups;
    @Autowired UserRepository users;

    @Test
    @DisplayName("survives a conversation that has no messages yet")
    void handlesGroupWithNoMessages() {
        User_ a = users.save(user("phase5-a"));
        User_ b = users.save(user("phase5-b"));

        Group_ group = groups.openDirectConversation(a.uid(), b.uid());
        assertThat(group.last_mid())
                .as("a freshly created DM has no messages")
                .isNull();

        List<ConversationDTO> result = messages.getConversations(a.uid());

        assertThat(result)
                .as("the empty conversation should simply not appear, "
                    + "rather than taking the whole list down")
                .isEmpty();
    }

    @Test
    @DisplayName("marks a conversation unread for the person who did not send the last message")
    void unreadIsPerViewer() {
        User_ sender = users.save(user("unread-sender"));
        User_ reader = users.save(user("unread-reader"));

        Group_ direct = groups.openDirectConversation(sender.uid(), reader.uid());
        messages.sendMessage(direct.gid(), sender.uid(), "did you see this", 0);

        assertThat(conversationFor(reader, direct).unread())
                .as("the recipient has not read it, so it is unread for them")
                .isTrue();

        assertThat(conversationFor(sender, direct).unread())
                .as("your own message is never unread for you")
                .isFalse();
    }

    @Test
    @DisplayName("clears once the reader has opened the conversation")
    void openingTheConversationClearsUnread() {
        User_ sender = users.save(user("read-a"));
        User_ reader = users.save(user("read-b"));

        Group_ direct = groups.openDirectConversation(sender.uid(), reader.uid());
        messages.sendMessage(direct.gid(), sender.uid(), "first", 0);

        assertThat(conversationFor(reader, direct).unread()).isTrue();

        // What the client calls on opening a conversation. It used to walk every
        // unread message and flip a column on each; it records one timestamp now.
        messages.updatePrevReadReceipts(reader.uid(), direct.gid());

        assertThat(conversationFor(reader, direct).unread())
                .as("nothing has arrived since they last looked")
                .isFalse();

        messages.sendMessage(direct.gid(), sender.uid(), "second", 0);

        assertThat(conversationFor(reader, direct).unread())
                .as("but something arriving afterwards is unread again")
                .isTrue();
    }

    @Test
    @DisplayName("holds a group unread until every member has caught up")
    void groupIsReadOnlyWhenEveryoneHasRead() {
        User_ sender = users.save(user("group-read-a"));
        User_ first = users.save(user("group-read-b"));
        User_ second = users.save(user("group-read-c"));

        Group_ group = groups.createGroup("read state", sender.uid(),
                List.of(first.uid(), second.uid()));
        messages.sendMessage(group.gid(), sender.uid(), "everyone see this", 0);

        assertThat(seenBySender(sender, group))
                .as("neither of them has opened it yet")
                .isFalse();

        messages.updatePrevReadReceipts(first.uid(), group.gid());

        // The old per-message boolean could not express this at all: it recorded
        // one recipient's state and silently stood in for the rest.
        assertThat(seenBySender(sender, group))
                .as("one of two having read it is not everyone")
                .isFalse();

        messages.updatePrevReadReceipts(second.uid(), group.gid());

        assertThat(seenBySender(sender, group))
                .as("now that both have, the sender's message is seen")
                .isTrue();
    }

    /** The sender's seen-tick: has everyone else read the message they sent. */
    private boolean seenBySender(User_ sender, Group_ group) {
        return messages.getChatMessages(group.gid(), sender.uid()).get(0).is_read();
    }

    @Test
    @DisplayName("sends the unread flag to the client under that name")
    void unreadIsSerialised() throws Exception {
        User_ sender = users.save(user("unread-json-a"));
        User_ reader = users.save(user("unread-json-b"));

        Group_ direct = groups.openDirectConversation(sender.uid(), reader.uid());
        messages.sendMessage(direct.gid(), sender.uid(), "bold me", 0);

        // The client styles the row from this field, so the name it arrives under
        // matters as much as the value.
        String json = new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsString(conversationFor(reader, direct));

        assertThat(json).contains("\"unread\":true");
    }

    private ConversationDTO conversationFor(User_ viewer, Group_ group) {
        return messages.getConversations(viewer.uid()).stream()
                .filter(c -> c.gid().equals(group.gid()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("conversation missing from the list"));
    }

    @Test
    @DisplayName("pairs each conversation with its own membership row")
    void pairsGroupsWithTheRightMembership() {
        User_ a = users.save(user("phase5-c"));
        User_ b = users.save(user("phase5-d"));
        User_ c = users.save(user("phase5-e"));

        Group_ withB = groups.openDirectConversation(a.uid(), b.uid());
        Group_ withC = groups.openDirectConversation(a.uid(), c.uid());

        send(withB.gid(), a.uid(), "to-b");
        send(withC.gid(), a.uid(), "to-c");

        List<ConversationDTO> result = messages.getConversations(a.uid());

        assertThat(result).hasSize(2);
        // Each row must name the other participant of its own group.
        assertThat(result).allSatisfy(dto -> {
            Long expectedOther = dto.gid().equals(withB.gid()) ? b.uid() : c.uid();
            assertThat(dto.uid())
                    .as("conversation %s should be with user %s", dto.gid(), expectedOther)
                    .isEqualTo(expectedOther);
        });
    }

    private void send(Long gid, Long from, String text) {
        messages.sendMessage(gid, from, text, 0);
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
