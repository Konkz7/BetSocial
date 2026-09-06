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
 * createDMGroup produces - therefore breaks the caller's entire conversation
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

        Group_ group = groups.createDMGroup(a.uid() + "" + b.uid(), a.uid(), b.uid());
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
    @DisplayName("pairs each conversation with its own membership row")
    void pairsGroupsWithTheRightMembership() {
        User_ a = users.save(user("phase5-c"));
        User_ b = users.save(user("phase5-d"));
        User_ c = users.save(user("phase5-e"));

        Group_ withB = groups.createDMGroup("ab", a.uid(), b.uid());
        Group_ withC = groups.createDMGroup("ac", a.uid(), c.uid());

        send(withB.gid(), a.uid(), b.uid(), "to-b");
        send(withC.gid(), a.uid(), c.uid(), "to-c");

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

    private void send(Long gid, Long from, Long to, String text) {
        messages.sendMessage(gid, from, to, text, 0);
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
