package com.example.World.Blocks;

import com.example.World.Follows.FollowRepository;
import com.example.World.Users.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Who can see whom.
 *
 * The single place that answers it, so a new screen has one method to call
 * rather than a rule to reimplement. Everything here works in terms of "the set
 * of people invisible to this viewer", because that is the shape every caller
 * actually needs - a filter over a list it already has.
 */
@Service
public class BlockService {

    private final BlockRepository blockRepository;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    public BlockService(BlockRepository blockRepository, UserRepository userRepository,
                        FollowRepository followRepository) {
        this.blockRepository = blockRepository;
        this.userRepository = userRepository;
        this.followRepository = followRepository;
    }

    /** Everybody this viewer cannot see, and who cannot see them. */
    public Set<Long> invisibleTo(Long uid) {
        return new HashSet<>(blockRepository.invisibleTo(uid));
    }

    /** Whether these two have blocked each other, either way round. */
    public boolean blockedBetween(Long a, Long b) {
        return blockRepository.exists(a, b) || blockRepository.exists(b, a);
    }

    /**
     * Refuses an interaction between two people where either has blocked the
     * other. 404 rather than 403 on purpose: telling somebody they have been
     * blocked is itself a message from a person who chose to stop receiving
     * them.
     */
    public void requireNotBlocked(Long viewer, Long other) {
        if (blockedBetween(viewer, other)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
    }

    public void block(Long blocker, Long blocked) {
        if (blocker.equals(blocked)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot block yourself");
        }
        if (userRepository.findById(blocked).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        // Blocking twice is the same block. Saying so rather than failing on the
        // unique constraint keeps the button idempotent.
        if (blockRepository.exists(blocker, blocked)) {
            return;
        }
        blockRepository.save(new Block_(null, blocker, blocked, new Date().getTime()));

        // Any follow between them goes too, in both directions. Left in place, a
        // blocked person still counts towards your follower count and still forms
        // the mutual follow that makes a private thread visible.
        followRepository.deleteBetween(blocker, blocked);
    }

    public void unblock(Long blocker, Long blocked) {
        blockRepository.unblock(blocker, blocked);
    }

    public List<Block_> blockedBy(Long uid) {
        return blockRepository.blockedBy(uid);
    }
}
