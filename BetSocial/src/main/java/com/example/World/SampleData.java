package com.example.World;

import com.example.World.Groups.GroupRepository;
import com.example.World.Groups.GroupService;
import com.example.World.Groups.Group_;
import com.example.World.Messages.MessageRepository;
import com.example.World.Messages.Message_;
import com.example.World.Threads.ThreadRepository;
import com.example.World.Threads.Thread_;
import com.example.World.Users.UserRepository;
import com.example.World.Users.User_;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

/**
 * Enough content to actually see the behaviour that only appears at volume.
 *
 * Eleven users and a handful of threads never cross a twenty-row page boundary,
 * so pagination cannot be tested by hand however green the suite is - the second
 * page simply does not exist. The same is true of anything else whose behaviour
 * depends on there being a lot of something: rate limiting, notification
 * fan-out, feed performance.
 *
 * Never runs on its own. There are two ways to ask for it, because the first one
 * on its own was not good enough:
 *
 *   POST /superusers/sample-data      - as an admin, no restart needed
 *   SAMPLE_DATA=true ./mvnw spring-boot:run
 *
 * The property has to be set before the backend starts and does not reach an IDE
 * run configuration - the README says the same about DB_PASSWORD - so using it
 * meant remembering to launch a particular way, and forgetting looked exactly
 * like the feature not working. The endpoint is the one to reach for; the
 * property is for a scripted setup.
 *
 * Everything here tops up to a target rather than appending, so asking twice
 * changes nothing the second time.
 */
@Component
public class SampleData {

    private static final Logger log = LoggerFactory.getLogger(SampleData.class);

    /** Comfortably past the feed's 20-row page, so there are several pages. */
    private static final int TARGET_THREADS = 75;

    /** Past a conversation page too, with room to scroll back through several. */
    private static final int TARGET_MESSAGES_PER_CONVERSATION = 120;

    /** The name of the long sample conversation, used to avoid making a second one. */
    private static final String CONVERSATION_NAME = "Sample long chat";

    /** Enough accounts that a user list or search has to do something. */
    private static final int TARGET_USERS = 60;

    /**
     * Fixed seed. Sample data that changes every run makes "it looked different
     * yesterday" impossible to answer.
     */
    private static final Random RANDOM = new Random(20260912L);

    private static final String[] CATEGORIES =
            {"Sports", "Politics", "Entertainment", "Tech", "Gaming"};

    private static final String[] SUBJECTS = {
            "Arsenal", "the election", "the new console", "this year's finals",
            "the interest rate", "that transfer rumour", "the album", "the merger",
            "the weather in March", "the title race", "the launch date",
    };

    private static final String[] CLAIMS = {
            "is going to disappoint everyone", "will be fine, actually",
            "is the most overrated thing this year", "cannot possibly hold up",
            "is worth paying attention to", "has been called far too early",
            "will look obvious in hindsight", "is a coin flip",
    };

    private final UserRepository userRepository;
    private final ThreadRepository threadRepository;
    private final MessageRepository messageRepository;
    private final GroupService groupService;
    private final GroupRepository groupRepository;
    private final boolean enabled;

    public SampleData(UserRepository userRepository, ThreadRepository threadRepository,
                      MessageRepository messageRepository, GroupService groupService,
                      GroupRepository groupRepository,
                      @Value("${betsocial.sample-data:false}") boolean enabled) {
        this.userRepository = userRepository;
        this.threadRepository = threadRepository;
        this.messageRepository = messageRepository;
        this.groupService = groupService;
        this.groupRepository = groupRepository;
        this.enabled = enabled;
    }

    /**
     * Runs after Startup, because it needs the seeded accounts to hang content
     * off. ApplicationReadyEvent listeners run in declaration order within a
     * bean, but not across beans - so this reads the users it needs rather than
     * assuming Startup has finished, and does nothing if there are none.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!enabled) {
            return;
        }
        generate();
    }

    /**
     * Creates the sample data, whoever asked for it.
     *
     * Separate from the startup hook because the environment variable turned out
     * to be the wrong switch: it does not reach an IDE run configuration - the
     * README says so about DB_PASSWORD for the same reason - so switching it on
     * meant remembering to launch the backend a particular way, which is exactly
     * the kind of thing that gets forgotten. An admin can now ask for it while
     * the server is running, and the property is kept for a scripted setup.
     *
     * Returns what it did rather than nothing, so the caller can say so.
     */
    public String generate() {
        List<User_> existing = userRepository.findAll().stream()
                .filter(u -> u.deleted_at() == null)
                .toList();

        if (existing.isEmpty()) {
            log.warn("Sample data was asked for but there are no accounts to attach it to.");
            return "No accounts to attach sample data to.";
        }

        int usersBefore = existing.size();
        List<User_> people = topUpUsers(existing);
        int threadsAdded = topUpThreads(people);
        int messagesAdded = topUpConversation(people);

        return "Added " + (people.size() - usersBefore) + " accounts, "
                + threadsAdded + " threads and " + messagesAdded + " messages.";
    }

    private List<User_> topUpUsers(List<User_> existing) {
        int missing = TARGET_USERS - existing.size();
        if (missing <= 0) {
            return existing;
        }

        // A block of phone numbers and emails that cannot collide with the
        // seeded accounts or with the test fixtures, which use +2.34e12 upwards.
        for (int i = 0; i < missing; i++) {
            long seq = 2_360_000_000_000L + existing.size() + i;
            String name = "sample" + (existing.size() + i);
            userRepository.save(new User_(null, name, name + "@example.test",
                    // A known-invalid hash: these accounts are here to be listed
                    // and searched, not signed in to. Giving them the same
                    // password as the demo users would quietly create sixty more
                    // ways into the app.
                    "not-a-usable-password",
                    "+" + seq, null, true, "", null, System.currentTimeMillis(), null,
                    0, null, "offline", null, 0.0, null));
        }
        log.info("Sample data: added {} accounts.", missing);

        return userRepository.findAll().stream().filter(u -> u.deleted_at() == null).toList();
    }

    private int topUpThreads(List<User_> people) {
        long live = threadRepository.findAll().stream()
                .filter(t -> t.deleted_at() == null)
                .count();

        int missing = (int) (TARGET_THREADS - live);
        if (missing <= 0) {
            return 0;
        }

        // Spread backwards in time, a few minutes apart, so the feed has a real
        // ordering to page through rather than seventy rows sharing a timestamp.
        long now = System.currentTimeMillis();

        for (int i = 0; i < missing; i++) {
            User_ author = people.get(RANDOM.nextInt(people.size()));
            threadRepository.save(new Thread_(null, author.uid(), headline(), null, 0,
                    CATEGORIES[RANDOM.nextInt(CATEGORIES.length)], 0L,
                    now - (long) i * 7 * 60 * 1000, null,
                    // A handful private, so the visibility rules have something
                    // to do on a feed this size.
                    RANDOM.nextInt(10) == 0, null));
        }
        log.info("Sample data: added {} threads.", missing);
        return missing;
    }

    private String headline() {
        return SUBJECTS[RANDOM.nextInt(SUBJECTS.length)] + " " + CLAIMS[RANDOM.nextInt(CLAIMS.length)];
    }

    /**
     * One long conversation, so a chat screen has more than a screenful of
     * history to scroll back through.
     */
    private int topUpConversation(List<User_> people) {
        if (people.size() < 3) {
            return 0;
        }

        // Idempotent like the other two. Without this, asking twice would create
        // a second identical conversation - which matters far more now that
        // asking is a button rather than a restart.
        boolean alreadyThere = groupRepository.findAll().stream()
                .anyMatch(group -> CONVERSATION_NAME.equals(group.group_name()));
        if (alreadyThere) {
            return 0;
        }

        User_ owner = people.get(0);
        List<Long> others = List.of(people.get(1).uid(), people.get(2).uid());

        Group_ group = groupService.createGroup(CONVERSATION_NAME, owner.uid(), others);

        long now = System.currentTimeMillis();
        List<Long> members = List.of(owner.uid(), others.get(0), others.get(1));

        // Written straight through the repository rather than MessageService:
        // that path sends a push notification and a websocket frame per message,
        // which for a hundred and twenty of them at startup is a lot of noise and
        // a lot of pointless work.
        for (int i = 0; i < TARGET_MESSAGES_PER_CONVERSATION; i++) {
            Long sender = members.get(i % members.size());
            messageRepository.save(new Message_(null, sender,
                    "Sample message " + (i + 1), 0,
                    now - (long) (TARGET_MESSAGES_PER_CONVERSATION - i) * 60 * 1000,
                    null, group.gid(), null));
        }

        log.info("Sample data: added a conversation ({}) with {} messages.",
                group.gid(), TARGET_MESSAGES_PER_CONVERSATION);

        return TARGET_MESSAGES_PER_CONVERSATION;
    }
}
