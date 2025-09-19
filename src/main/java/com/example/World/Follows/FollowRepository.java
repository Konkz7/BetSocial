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



}
