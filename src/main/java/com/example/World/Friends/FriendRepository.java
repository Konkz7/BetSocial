package com.example.World.Friends;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface FriendRepository extends JpaRepository<Friendship_, Long> {

    List<Friendship_> findByReceiverIdAndStatus(Long receiverId, Integer friendship);
    List<Friendship_> findByRequesterIdAndStatus(Long requesterId, Integer friendship);
    boolean existsByRequesterIdAndReceiverId(Long requesterId, Long receiverId);

    @Modifying
    @Transactional
    @Query("UPDATE Friendship_ SET friendship = :friendship WHERE fid = :fid")
    int updateFriendship(@Param("fid") Long fid, @Param("friendship") Integer friendship);


}
