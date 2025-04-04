package com.example.World.Friends;

import com.example.World.Users.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FriendService {

    private final FriendRepository friendRepository;
    private final UserRepository userRepository;

    FriendService(FriendRepository friendRepository, UserRepository userRepository){
        this.friendRepository = friendRepository;
        this.userRepository = userRepository;
    }
    public ResponseEntity<String> sendFriendRequest(Long requesterId, Long receiverId) {
        if (friendRepository.existsByRequestIdAndReceiveId(requesterId, receiverId)) {
            return ResponseEntity.badRequest().body("Friend request already sent!");
        }
        Long request_id = userRepository.findById(requesterId).orElseThrow().uid();
        Long recieve_id = userRepository.findById(receiverId).orElseThrow().uid();
        Integer status = Stage.PENDING.toInt();
        Friendship_ friend = new Friendship_(null,request_id,recieve_id,null,status);

        friendRepository.save(friend);
        return ResponseEntity.ok().body("Friend request sent!");
    }

    public ResponseEntity<String>  Unfriend(Long requesterId, Long receiverId) {
        try {
            Friendship_ friend = friendRepository.findByRequestIdAndReceiveId(requesterId,receiverId).orElseThrow();
            friendRepository.delete(friend);
        }catch ( Exception e){
            return ResponseEntity.badRequest().body("Friend request doesn't exist!");
        }
        return ResponseEntity.ok().body("Friendship terminated!");
    }


    public ResponseEntity<String>  acceptFriendRequest(Long friendshipId) {
        try {
            Friendship_ friendship = friendRepository.findById(friendshipId).orElseThrow();
        }catch ( Exception e){
            return ResponseEntity.badRequest().body("Friend request already sent!");
        }
        friendRepository.updateFriendship(friendshipId,Stage.ACCEPTED.toInt());
        return ResponseEntity.ok().body("Friend request accepted!");
    }

    public ResponseEntity<String>  rejectFriendRequest(Long friendshipId) {
        try {
            Friendship_ friendship = friendRepository.findById(friendshipId).orElseThrow();
            friendRepository.delete(friendship);
        }catch ( Exception e){
            return ResponseEntity.badRequest().body("Friend request doesn't exist!");
        }
        return ResponseEntity.ok().body("Friend request rejected!");
    }

    public List<Friendship_> getFriends(Long userId) {
        return friendRepository.findByIdAndStage(userId, Stage.ACCEPTED.toInt());
    }

    public Optional<Friendship_> getFriendship(Long requestId , Long receiveId) {
        return friendRepository.findByRequestIdAndReceiveId(requestId,receiveId);
    }

    public List<Friendship_> getReceivedFriendRequests(Long userId) {
        return friendRepository.findByReceiveIdAndStage(userId, Stage.PENDING.toInt());
    }

    public List<Friendship_> getSentFriendRequests(Long userId) {
        return friendRepository.findByRequestIdAndStage(userId, Stage.PENDING.toInt());
    }
}
