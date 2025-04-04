package com.example.World.Friends;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<String>  sendFriendRequest(@PathVariable Long receiverId , HttpSession session) {
        Long uid = (Long) session.getAttribute("userId");
        return friendService.sendFriendRequest(uid, receiverId);
    }

    @PutMapping("/accept/{friendshipId}")
    public ResponseEntity<String>  acceptFriendRequest(@PathVariable Long friendshipId) {
        return friendService.acceptFriendRequest(friendshipId);
    }

    @PutMapping("/reject/{friendshipId}")
    public ResponseEntity<String>  rejectFriendRequest(@PathVariable Long friendshipId) {
        return friendService.rejectFriendRequest(friendshipId);
    }

    @DeleteMapping("/unfriend/{ouid}")
    public ResponseEntity<String> Unfriend (@PathVariable Long ouid , HttpSession session){
        Long uid = (Long) session.getAttribute("userId");
        return friendService.Unfriend(uid,ouid);
    }


    @GetMapping("/friendship/{ouid}")
    public Friendship_ getFriendship(@PathVariable Long ouid , HttpSession session) {
        Long uid = (Long) session.getAttribute("userId");
        return friendService.getFriendship(uid, ouid).isEmpty() ? null : friendService.getFriendship(uid, ouid).get();
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
