package com.example.World.Blocks;

import com.example.World.Users.UserRepository;
import com.example.World.Users.UserView;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Blocking and unblocking.
 *
 * There is no endpoint for reading anybody else's block list - who you have
 * blocked is nobody's business but yours, and publishing it would tell a
 * blocked person exactly where they stand.
 */
@RequestMapping("/api/blocks")
@RestController
public class BlockController {

    private final BlockService blockService;
    private final UserRepository userRepository;

    public BlockController(BlockService blockService, UserRepository userRepository) {
        this.blockService = blockService;
        this.userRepository = userRepository;
    }

    /** The people you have blocked, so the settings screen can list and undo them. */
    @GetMapping
    List<UserView> myBlocks(HttpSession session) {
        Long uid = requireUserId(session);
        return blockService.blockedBy(uid).stream()
                .map(Block_::blocked_uid)
                .map(userRepository::findById)
                .flatMap(java.util.Optional::stream)
                .map(UserView::from)
                .toList();
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/{uid}")
    void block(@PathVariable Long uid, HttpSession session) {
        blockService.block(requireUserId(session), uid);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{uid}")
    void unblock(@PathVariable Long uid, HttpSession session) {
        blockService.unblock(requireUserId(session), uid);
    }

    private static Long requireUserId(HttpSession session) {
        Long uid = (Long) session.getAttribute("userId");
        if (uid == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        return uid;
    }
}
