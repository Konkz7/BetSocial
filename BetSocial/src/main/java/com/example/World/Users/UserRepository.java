package com.example.World.Users;


import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Optional;

public interface UserRepository extends ListCrudRepository<User_, Long> {

    @Query("SELECT * FROM User_ WHERE user_name = :username AND deleted_at IS NULL")
    Optional<User_> findByUsername(String username);

    @Query("SELECT * FROM User_ WHERE user_role = 0 AND uid != :uid AND deleted_at IS NULL")
    List<User_> findAllActiveUsers(@Param("uid") Long uid);

    /**
     * People matching a name, capped.
     *
     * This replaces handing back every account. Paging a user list was never the
     * right answer for the screens that used it - all three were choosing
     * somebody, and nobody scrolls to account four hundred to pick a friend -
     * so the fix is to search rather than to page.
     *
     * An empty term matches everyone, which is what a picker wants when it first
     * opens: a list to start from rather than a blank screen. The cap applies
     * either way.
     *
     * Blocked accounts are excluded in both directions, the same rule as the
     * feed. ILIKE rather than LOWER(...) LIKE LOWER(...) because it says what it
     * means and Postgres can use an index on it.
     */
    @Query("""
    SELECT u.* FROM User_ u
    WHERE u.user_role = 0
      AND u.uid != :viewer
      AND u.deleted_at IS NULL
      AND (:term = '' OR u.user_name ILIKE '%' || :term || '%')
      AND NOT EXISTS (
          SELECT 1 FROM Block_ b
          WHERE (b.blocker_uid = :viewer AND b.blocked_uid = u.uid)
             OR (b.blocker_uid = u.uid AND b.blocked_uid = :viewer))
    ORDER BY u.user_name ASC
    LIMIT :limit
    """)
    List<User_> search(@Param("viewer") Long viewer, @Param("term") String term,
                       @Param("limit") int limit);

    /**
     * Specific accounts by id.
     *
     * For screens that need to put a name to an id they already hold - the
     * activity list resolving who did the thing a notification is about - rather
     * than to browse. It used to fetch every account and search it in memory,
     * which quietly stopped working as soon as the list was capped.
     *
     * Soft-deleted accounts are left out, so a suspended account's name stops
     * appearing. Blocking is deliberately not applied here: a notification is a
     * record of something that already happened, and hiding only the name would
     * leave an unattributable row rather than removing it. Filtering
     * notifications by block is worth doing properly and separately.
     */
    @Query("SELECT * FROM User_ WHERE uid IN (:ids) AND deleted_at IS NULL")
    List<User_> findAllByIds(@Param("ids") List<Long> ids);

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

    @Modifying
    @Transactional
    @Query("UPDATE User_ SET bio = :bio WHERE uid = :id AND deleted_at IS NULL")
    int changeBio(@Param("id") Long id,@Param("bio") String bio);

    @Modifying
    @Transactional
    @Query("UPDATE User_ SET user_name = :user_name WHERE uid = :id AND deleted_at IS NULL")
    int changeUserName(@Param("id") Long id,@Param("user_name") String user_name);

    @Modifying
    @Transactional
    @Query("UPDATE User_ SET profile_picture = :profile_picture WHERE uid = :id AND deleted_at IS NULL")
    int changeProfilePicture(@Param("id") Long id,@Param("profile_picture") String profile_picture);


    @Modifying
    @Transactional
    @Query("UPDATE User_ SET fb_notification_token = :FBNtoken WHERE uid = :id AND deleted_at IS NULL")
    int saveFBNtoken(@Param("id") Long id,@Param("FBNtoken") String FBNtoken);

    @Query("SELECT * FROM User_ WHERE fb_notification_token = :token AND deleted_at IS NULL")
    List<User_> findByFBNToken(@Param("token") String token);

    @Modifying
    @Transactional
    @Query("UPDATE User_ SET status = :status WHERE uid = :id AND deleted_at IS NULL")
    int changeStatus(@Param("id") Long id,@Param("status") String status);

    /**
     * Suspends an account by soft-deleting it.
     *
     * Every lookup in this repository already filters deleted_at IS NULL,
     * including findByUsername - which is what UserService.loadUserByUsername
     * uses - so a suspended account cannot sign in and does not appear anywhere.
     * Nothing is destroyed: the row, its threads and its ledger stay, so the
     * decision can be reversed and the history still reads.
     */
    @Modifying
    @Transactional
    @Query("UPDATE User_ SET deleted_at = :time WHERE uid = :id AND deleted_at IS NULL")
    int suspend(@Param("id") Long id, @Param("time") Long time);

    /**
     * Removes the person from the account, leaving the account.
     *
     * One statement rather than a read-modify-write, so there is no window in
     * which half the fields are scrubbed. The row itself stays because every
     * retained thread, comment, message and ledger entry points at this uid -
     * deleting it cascades through all of them, which is what the old endpoint
     * did.
     *
     * Everything nulled here is personal data: the verification token and push
     * token are credentials, the picture and bio are content about the person,
     * and the password is replaced with a value no hash can match so the account
     * cannot be signed into even if deleted_at were ever cleared by hand.
     */
    @Modifying
    @Transactional
    @Query("""
    UPDATE User_ SET
        user_name = :scrubbedName,
        email = :scrubbedEmail,
        phone_number = :scrubbedPhone,
        pass_word = 'deleted-account-no-login',
        bio = '',
        profile_picture = NULL,
        verification_token = NULL,
        fb_notification_token = NULL,
        wallet_address = NULL,
        status = 'offline',
        is_verified = false,
        deleted_at = :time
    WHERE uid = :id AND deleted_at IS NULL
    """)
    int scrubPersonalData(@Param("id") Long id,
                          @Param("scrubbedName") String scrubbedName,
                          @Param("scrubbedEmail") String scrubbedEmail,
                          @Param("scrubbedPhone") String scrubbedPhone,
                          @Param("time") Long time);
}
