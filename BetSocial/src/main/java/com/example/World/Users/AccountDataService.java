package com.example.World.Users;

import com.example.World.Blocks.BlockRepository;
import com.example.World.Comments.CommentRepository;
import com.example.World.Follows.FollowRepository;
import com.example.World.Messages.MessageRepository;
import com.example.World.Predictions.PredictionRepository;
import com.example.World.Threads.ThreadRepository;
import com.example.World.Wallet.LedgerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The two things UK GDPR gives a user over their own account: a copy of their
 * data, and the right to have it erased.
 *
 * Neither existed. /api/users/delete hard-deleted the row, and almost every
 * table referencing user_ cascades - so it took the person's threads, comments,
 * messages, predictions, follows and, worst of all, their ledger entries. It
 * also failed outright with a constraint violation for anybody who had ever
 * decided a bet outcome, because decision_log does not cascade.
 */
@Service
public class AccountDataService {

    private static final Logger log = LoggerFactory.getLogger(AccountDataService.class);

    private final UserRepository userRepository;
    private final ThreadRepository threadRepository;
    private final CommentRepository commentRepository;
    private final MessageRepository messageRepository;
    private final PredictionRepository predictionRepository;
    private final LedgerRepository ledgerRepository;
    private final FollowRepository followRepository;
    private final BlockRepository blockRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountDataService(UserRepository userRepository, ThreadRepository threadRepository,
                              CommentRepository commentRepository, MessageRepository messageRepository,
                              PredictionRepository predictionRepository, LedgerRepository ledgerRepository,
                              FollowRepository followRepository, BlockRepository blockRepository,
                              PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.threadRepository = threadRepository;
        this.commentRepository = commentRepository;
        this.messageRepository = messageRepository;
        this.predictionRepository = predictionRepository;
        this.ledgerRepository = ledgerRepository;
        this.followRepository = followRepository;
        this.blockRepository = blockRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // --- right of access --------------------------------------------------

    /**
     * Everything held about one person, in one response.
     *
     * A map rather than a typed record because the shape is "whatever we hold",
     * which changes whenever a table is added - and a record would have to be
     * edited in step or quietly omit the new thing. Omitting something is the
     * one failure mode that matters here.
     *
     * The password hash is not included. It is data about the account rather
     * than about the person, and handing somebody their own hash only helps
     * whoever has taken their session.
     */
    public Map<String, Object> export(Long uid) {
        User_ user = userRepository.findById(uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("uid", user.uid());
        profile.put("user_name", user.user_name());
        profile.put("email", user.email());
        profile.put("phone_number", user.phone_number());
        profile.put("bio", user.bio());
        profile.put("profile_picture", user.profile_picture());
        profile.put("is_verified", user.is_verified());
        profile.put("created_at", user.created_at());

        Map<String, Object> export = new LinkedHashMap<>();
        export.put("exported_at", new Date().getTime());
        export.put("profile", profile);
        export.put("threads", threadRepository.findAllUserThreads(uid));
        export.put("comments", commentRepository.findAllByUser(uid));
        export.put("messages", messageRepository.findAllByUser(uid));
        export.put("predictions", predictionRepository.findByUid(uid));
        export.put("wallet", ledgerRepository.historyOf(uid, 10_000));
        export.put("following", followRepository.findByRequestId(uid));
        export.put("followers", followRepository.findByReceiveId(uid));
        export.put("blocked", blockRepository.blockedBy(uid));

        log.info("Exported account data for user {}", uid);
        return export;
    }

    // --- right to erasure -------------------------------------------------

    /**
     * Scrubs the person out of the account and leaves the content behind.
     *
     * Erasure under GDPR covers personal data, not every trace that somebody
     * existed. Their messages are half of somebody else's conversation and their
     * comments are part of somebody else's thread; deleting those erases other
     * people's records as a side effect. So the identity goes and the content
     * stays, attributed to a tombstone - see UserView.
     *
     * The ledger stays too, and that is not a preference. A settled bet's payout
     * was computed from pools this person's stake contributed to; removing the
     * stake row makes other people's balances unexplainable, which is precisely
     * what the append-only ledger exists to prevent.
     *
     * Transactional, and public so the annotation takes effect - a half-scrubbed
     * account is one that cannot log in and still has an email on it.
     */
    @Transactional
    public void deleteAccount(Long uid, String password) {
        User_ user = userRepository.findById(uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        if (user.deleted_at() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That account is already deleted");
        }

        // The password is required because this cannot be undone and a session is
        // easier to come by than a password - a borrowed phone should not be
        // enough to delete somebody's account.
        if (password == null || !passwordEncoder.matches(password, user.pass_word())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "That password is not right");
        }

        long now = new Date().getTime();

        // Unique columns, so the scrubbed values have to be unique too. Derived
        // from the uid, which is the one identifier that has to stay - every
        // retained thread, comment and ledger row points at it.
        userRepository.scrubPersonalData(uid,
                "deleted-user-" + uid,
                "deleted-" + uid + "@invalid",
                "del-" + uid,
                now);

        // Relationships go: they describe a person who is no longer here, and a
        // deleted account sitting in somebody's follower count is noise.
        followRepository.deleteAllFor(uid);
        blockRepository.deleteAllFor(uid);

        log.info("Deleted account {} - personal data scrubbed, content retained", uid);
    }
}
