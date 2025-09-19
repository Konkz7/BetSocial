package com.example.World.Follows;

import com.example.World.Users.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    FollowService(FollowRepository followRepository, UserRepository userRepository){
        this.followRepository = followRepository;
        this.userRepository = userRepository;
    }
    public ResponseEntity<String> sendFollow(Long requesterId, Long receiverId) {
        if (followRepository.existsByRequestIdAndReceiveId(requesterId, receiverId)) {
            return ResponseEntity.badRequest().body("Friend request already sent!");
        }
        Long request_id = userRepository.findById(requesterId).orElseThrow().uid();
        Long recieve_id = userRepository.findById(receiverId).orElseThrow().uid();
        Follow_ friend = new Follow_(null,request_id,recieve_id);

        followRepository.save(friend);
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
