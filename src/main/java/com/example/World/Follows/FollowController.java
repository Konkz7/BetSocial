package com.example.World.Follows;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/follows")
public class FollowController {
    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @PostMapping("/send/{receiverId}")
    public ResponseEntity<String>  sendFollow(@PathVariable Long receiverId , HttpSession session) {
        Long uid = (Long) session.getAttribute("userId");
        return followService.sendFollow(uid, receiverId);
    }


    @DeleteMapping("/unfollow/{ouid}")
    public ResponseEntity<String> Unfollow (@PathVariable Long ouid , HttpSession session){
        Long uid = (Long) session.getAttribute("userId");
        return followService.Unfollow(uid,ouid);
    }


    @GetMapping("/follow/{ouid}")
    public Follow_ getFollow(@PathVariable Long ouid , HttpSession session) {
        Long uid = (Long) session.getAttribute("userId");
        return followService.getFollow(uid,ouid);
    }

    @GetMapping("/other-follow/{ouid}")
    public Follow_ getOtherFollow(@PathVariable Long ouid , HttpSession session) {
        Long uid = (Long) session.getAttribute("userId");
        return followService.getFollow(ouid,uid);
    }

    @GetMapping("/followers")
    public List<Follow_> getReceivedFollows(HttpSession session) {
        Long uid = (Long) session.getAttribute("userId");
        return followService.getFollowers(uid);
    }

    @GetMapping("/follows")
    public List<Follow_> getSentFollows(HttpSession session) {
        Long uid = (Long) session.getAttribute("userId");
        return followService.getFollows(uid);
    }

    @GetMapping("/followers/{ouid}")
    public List<Follow_> getReceivedFollows(@PathVariable Long ouid) {

        return followService.getFollowers(ouid);
    }

    @GetMapping("/follows/{ouid}")
    public List<Follow_> getSentFollows(@PathVariable Long ouid) {

        return followService.getFollows(ouid);
    }
}
