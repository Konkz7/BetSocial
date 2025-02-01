package com.example.World.Friends;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
public class FriendController {
    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    @PostMapping("/send/{receiverId}")
    public String sendFriendRequest(@PathVariable Long receiverId , HttpSession session) {
        Long uid = (Long) session.getAttribute("userId");
        return friendService.sendFriendRequest(uid, receiverId);
    }

    @PutMapping("/accept/{friendshipId}")
    public String acceptFriendRequest(@PathVariable Long friendshipId) {
        return friendService.acceptFriendRequest(friendshipId);
    }

    @PutMapping("/reject/{friendshipId}")
    public String rejectFriendRequest(@PathVariable Long friendshipId) {
        return friendService.rejectFriendRequest(friendshipId);
    }

    @GetMapping("/received")
    public List<Friendship_> getReceivedFriendRequests(HttpSession session) {
        Long uid = (Long) session.getAttribute("userId");
        return friendService.getReceivedFriendRequests(uid);
    }

    @GetMapping("/sent")
    public List<Friendship_> getSentFriendRequests(HttpSession session) {
        Long uid = (Long) session.getAttribute("userId");
        return friendService.getSentFriendRequests(uid);
    }


    @GetMapping("/list")
    public List<Friendship_> getFriends(HttpSession session) {
        Long uid = (Long) session.getAttribute("userId");
        return friendService.getFriends(uid);
    }
}
