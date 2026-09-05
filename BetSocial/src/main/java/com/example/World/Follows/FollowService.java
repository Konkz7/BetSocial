package com.example.World.Follows;

import com.example.World.Notifications.NotificationDTO;
import com.example.World.Notifications.NotificationService;
import com.example.World.Users.UserRepository;
import com.example.World.Users.User_;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    FollowService(FollowRepository followRepository, UserRepository userRepository, NotificationService notificationService){
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }
    public ResponseEntity<String> sendFollow(Long requesterId, Long receiverId) {
        if (followRepository.existsByRequestIdAndReceiveId(requesterId, receiverId)) {
            return ResponseEntity.badRequest().body("Friend request already sent!");
        }

        Follow_ friend = new Follow_(null,requesterId,receiverId);

        followRepository.save(friend);


        User_ target = userRepository.findById(receiverId).orElseThrow();

        NotificationDTO temp = new NotificationDTO(requesterId, "follow_request", receiverId, "user");

        notificationService.registerNotification(target.fb_notification_token(),
                "Send a follow back!",
                temp, receiverId);

        return ResponseEntity.ok().body("Friend request sent!");
    }

    public ResponseEntity<String>  Unfollow(Long requesterId, Long receiverId) {
        try {
            Follow_ friend = followRepository.findByRequestIdAndReceiveId(requesterId,receiverId).orElseThrow();
            followRepository.delete(friend);
        }catch ( Exception e){
            return ResponseEntity.badRequest().body("Friend request doesn't exist!");
        }
        return ResponseEntity.ok().body("Friendship terminated!");
    }

    public List<Follow_> getFollows(Long userId) {
        return followRepository.findByRequestId(userId);
    }

    public Follow_ getFollow(Long requestId , Long receiveId) {
        return followRepository.findByRequestIdAndReceiveId(requestId,receiveId).orElse(null);
    }


    public List<Follow_> getFollowers(Long userId) {
        return followRepository.findByReceiveId(userId);
    }


}
