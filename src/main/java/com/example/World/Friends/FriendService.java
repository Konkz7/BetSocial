package com.example.World.Friends;

import com.example.World.Users.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FriendService {

    private final FriendRepository friendRepository;
    private final UserRepository userRepository;

    FriendService(FriendRepository friendRepository, UserRepository userRepository){
        this.friendRepository = friendRepository;
        this.userRepository = userRepository;
    }
    public String sendFriendRequest(Long requesterId, Long receiverId) {
        if (friendRepository.existsByRequestIdAndReceiveId(requesterId, receiverId)) {
            return "Friend request already sent!";
        }
        Long request_id = userRepository.findById(requesterId).orElseThrow().uid();
        Long recieve_id = userRepository.findById(receiverId).orElseThrow().uid();
        Integer status = Stage.PENDING.toInt();
        Friendship_ friend = new Friendship_(null,request_id,recieve_id,null,status);

        friendRepository.save(friend);
        return "Friend request sent!";
    }


    public String acceptFriendRequest(Long friendshipId) {
        Friendship_ friendship = friendRepository.findById(friendshipId).orElseThrow();
        friendRepository.updateFriendship(friendshipId,Stage.ACCEPTED.toInt());
        return "Friend request accepted!";
    }

    public String rejectFriendRequest(Long friendshipId) {
        Friendship_ friendship = friendRepository.findById(friendshipId).orElseThrow();
        friendRepository.updateFriendship(friendshipId,Stage.REJECTED.toInt());
        return "Friend request accepted!";
    }

    public List<Friendship_> getFriends(Long userId) {
        return friendRepository.findByIdAndStage(userId, Stage.ACCEPTED.toInt());
    }

    public List<Friendship_> getReceivedFriendRequests(Long userId) {
        return friendRepository.findByReceiveIdAndStage(userId, Stage.PENDING.toInt());
    }

    public List<Friendship_> getSentFriendRequests(Long userId) {
        return friendRepository.findByRequestIdAndStage(userId, Stage.PENDING.toInt());
    }
}
