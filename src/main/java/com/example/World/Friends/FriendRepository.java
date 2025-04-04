package com.example.World.Friends;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


public interface FriendRepository extends ListCrudRepository<Friendship_, Long> {


    // Find friendships where the user is the recipient and the stage matches
    @Query(value = "SELECT * FROM Friendship_  WHERE receive_id = :receiveId AND stage = :stage")
    List<Friendship_> findByReceiveIdAndStage(@Param("receiveId") Long receiveId, @Param("stage") Integer stage);

    // Find friendships where the user is the requester and the stage matches
    @Query(value = "SELECT * FROM Friendship_  WHERE request_id = :requestId AND stage = :stage")
    List<Friendship_> findByRequestIdAndStage(@Param("requestId") Long requestId, @Param("stage") Integer stage);

    @Query(value = "SELECT * FROM Friendship_  WHERE (receive_id = :Id OR request_id = :Id) AND stage = :stage")
    List<Friendship_> findByIdAndStage(@Param("Id") Long Id, @Param("stage") Integer stage);

    // Check if a friendship exists between two users
    @Query(value = "SELECT EXISTS (SELECT 1 FROM Friendship_ WHERE request_id = :requestId AND receive_id = :receiveId)")
    boolean existsByRequestIdAndReceiveId(@Param("requestId") Long requestId, @Param("receiveId") Long receiveId);

    @Query(value = "SELECT * FROM Friendship_ WHERE (request_id = :requestId AND receive_id = :receiveId) OR " +
            "(request_id = :receiveId AND receive_id = :requestId) ")
    Optional<Friendship_> findByRequestIdAndReceiveId(@Param("requestId") Long requestId, @Param("receiveId") Long receiveId);

    @Modifying
    @Transactional
    @Query("UPDATE Friendship_ SET stage = :stage WHERE fid = :fid")
    int updateFriendship(@Param("fid") Long fid, @Param("stage") Integer stage);



}
