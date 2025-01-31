package com.example.World.Friends;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/friends")
public class FriendController {
    private FriendService friendService;

    @PostMapping("/send")
    public String sendFriendRequest(@RequestParam Long requesterId, @RequestParam Long receiverId) {
        return friendService.sendFriendRequest(requesterId, receiverId);
    }

    @PostMapping("/accept/{friendshipId}")
    public String acceptFriendRequest(@PathVariable Long friendshipId) {
        return friendService.acceptFriendRequest(friendshipId);
    }

    @PostMapping("/reject/{friendshipId}")
    public String rejectFriendRequest(@PathVariable Long friendshipId) {
        return friendService.rejectFriendRequest(friendshipId);
    }

    @GetMapping("/list/{userId}")
    public List<Friendship_> getFriends(@PathVariable Long userId) {
        return friendService.getFriends(userId);
    }
}
