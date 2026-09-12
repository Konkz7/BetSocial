package com.example.World.Follows;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


public interface FollowRepository extends ListCrudRepository<Follow_, Long> {


    // Find friendships where the user is the recipient and the stage matches
    @Query(value = "SELECT * FROM Follow_  WHERE receive_id = :receiveId ")
    List<Follow_> findByReceiveId(@Param("receiveId") Long receiveId);

    // Find friendships where the user is the requester and the stage matches
    @Query(value = "SELECT * FROM Follow_ WHERE request_id = :requestId ")
    List<Follow_> findByRequestId(@Param("requestId") Long requestId);

    // Check if a friendship exists between two users
    @Query(value = "SELECT EXISTS (SELECT 1 FROM Follow_ WHERE request_id = :requestId AND receive_id = :receiveId)")
    boolean existsByRequestIdAndReceiveId(@Param("requestId") Long requestId, @Param("receiveId") Long receiveId);

    @Query(value = "SELECT * FROM Follow_ WHERE request_id = :requestId AND receive_id = :receiveId")
    Optional<Follow_> findByRequestIdAndReceiveId(@Param("requestId") Long requestId, @Param("receiveId") Long receiveId);

    /**
     * Removes any follow between two people, whichever way round it points.
     *
     * Used when one blocks the other: leaving the rows in place would mean a
     * blocked person still appears in your follower count and still counts as a
     * mutual follow, which is what makes a private thread visible.
     */
    @Modifying
    @Transactional
    @Query("""
    DELETE FROM Follow_
    WHERE (request_id = :a AND receive_id = :b)
       OR (request_id = :b AND receive_id = :a)
    """)
    int deleteBetween(@Param("a") Long a, @Param("b") Long b);



}
