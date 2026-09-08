package com.example.World.Wallet;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LedgerRepository extends ListCrudRepository<LedgerEntry_, Long> {

    /**
     * The balance: the sum of everything that has moved.
     *
     * Derived rather than cached. A cached column has to be written in the same
     * transaction as every entry or the two drift apart, and a balance that
     * disagrees with its own history is worse than one that takes a moment to
     * add up. The index on (uid, created_at) covers this; if it ever stops being
     * fast enough, the cache goes on user_ and is written here, in one place.
     */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM Ledger_entry_ WHERE uid = :uid")
    long balanceOf(@Param("uid") Long uid);

    /** A user's movements, most recent first. */
    @Query("SELECT * FROM Ledger_entry_ WHERE uid = :uid ORDER BY created_at DESC, leid DESC LIMIT :limit")
    List<LedgerEntry_> historyOf(@Param("uid") Long uid, @Param("limit") int limit);

    /** The most recent entry of one kind, used to decide whether a top-up is due. */
    @Query("""
    SELECT * FROM Ledger_entry_
    WHERE uid = :uid AND reason = :reason
    ORDER BY created_at DESC, leid DESC
    LIMIT 1
    """)
    Optional<LedgerEntry_> latestOf(@Param("uid") Long uid, @Param("reason") String reason);

    /** Every stake still standing on a bet - what settlement has to give back or pay out on. */
    @Query("SELECT * FROM Ledger_entry_ WHERE bid = :bid AND reason = 'STAKE'")
    List<LedgerEntry_> stakesOn(@Param("bid") Long bid);
}
