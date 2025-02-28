package com.example.World.Users;


import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;


import java.util.Optional;

public interface UserRepository extends ListCrudRepository<User_, Long> {

    @Query("SELECT * FROM User_ WHERE user_name = :username AND deleted_at IS NULL")
    Optional<User_> findByUsername(String username);

    @Query("SELECT * FROM User_ WHERE email = :email AND deleted_at IS NULL")
    Optional<User_> findByEmail(String email);

    @Query("SELECT * FROM User_ WHERE verification_token = :token AND deleted_at IS NULL")
    Optional<User_> findByVerificationToken(String token);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM User_ WHERE phone_number = :phone_number AND deleted_at IS NULL)")
    boolean existsByPhoneNumber(@Param("phone_number") String phone_number);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM User_ WHERE email = :email AND deleted_at IS NULL)")
    boolean existsByEmail(@Param("email") String email);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM User_ WHERE user_name = :user_name AND deleted_at IS NULL)")
    boolean existsByUserName(@Param("user_name") String user_name);

    @Modifying
    @Transactional
    @Query("UPDATE User_ SET is_verified = true , verification_token = NULL WHERE uid = :id AND deleted_at IS NULL")
    int verify(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE User_ SET wallet_address = :wallet_address WHERE uid = :id AND deleted_at IS NULL")
    int setWalletAddress(@Param("id") Long id,@Param("wallet_address") String wallet_address);

}
