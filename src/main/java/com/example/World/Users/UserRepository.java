package com.example.World.Users;


import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends ListCrudRepository<User_, Long> {

    @Query("SELECT * FROM User_ WHERE user_name = :username AND deleted_at IS NULL")
    Optional<User_> findByUsername(String username);


}
