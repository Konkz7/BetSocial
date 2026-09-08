package com.example.World.Wallet;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;

/**
 * The only place coins move.
 *
 * Everything goes through {@link #record}, so there is one answer to "what
 * changed this balance" and it is always "a row in the ledger". Nothing edits a
 * balance directly, because there is no balance to edit - it is the sum of the
 * entries.
 */
@Service
public class LedgerService {

    /** What a new account starts with. */
    public static final long OPENING_GRANT = 1_000L;

    /** What it gains each day, so that running out costs a day rather than the account. */
    public static final long DAILY_TOPUP = 100L;

    private static final long ONE_DAY_MILLIS = 24L * 60 * 60 * 1000;

    /** Enough to fill the wallet screen without paging; the rest is history nobody scrolls to. */
    private static final int HISTORY_LIMIT = 100;

    private final LedgerRepository ledgerRepository;

    public LedgerService(LedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    /**
     * Writes one movement.
     *
     * Deliberately the only way in. A debit is a negative amount and nothing here
     * checks whether the balance can stand it - that is the caller's decision to
     * make before it commits, because only the caller knows what to say when it
     * cannot. See {@link #requireBalance}.
     */
    @Transactional
    public LedgerEntry_ record(Long uid, long amount, LedgerReason reason, Long bid, String description) {
        if (amount == 0) {
            throw new IllegalArgumentException("A ledger entry moves coins; " + reason + " moved none");
        }

        return ledgerRepository.save(new LedgerEntry_(
                null, uid, amount, reason.name(), bid, description, new Date().getTime()));
    }

    public long balanceOf(Long uid) {
        return ledgerRepository.balanceOf(uid);
    }

    /**
     * Refuses a spend the balance cannot cover.
     *
     * Checked here rather than left to a database constraint because the answer is
     * a 400 with a number in it, not a failed insert - and because a balance is a
     * sum rather than a column, so there is nothing for a constraint to sit on.
     */
    public void requireBalance(Long uid, long amount) {
        long balance = balanceOf(uid);
        if (balance < amount) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "That would need " + amount + " coins and you have " + balance);
        }
    }

    /** Called once, when an account is created. */
    @Transactional
    public void grantOpeningBalance(Long uid) {
        record(uid, OPENING_GRANT, LedgerReason.OPENING_GRANT, null, "Opening grant");
    }

    /**
     * Adds the daily top-up if a day has passed since the last one.
     *
     * Worked out from the ledger rather than tracked on the user, so there is no
     * second place to keep in step. It is also why this is not a scheduled job:
     * one of those grants nothing for the days the server was not running, while
     * this catches up whenever somebody next looks.
     *
     * A day, not a calendar date - a user who opens the app at eleven each night
     * should not get two top-ups in three hours.
     */
    @Transactional
    public void topUpIfDue(Long uid) {
        long now = new Date().getTime();

        boolean due = ledgerRepository.latestOf(uid, LedgerReason.DAILY_TOPUP.name())
                .map(latest -> now - latest.created_at() >= ONE_DAY_MILLIS)
                // Never topped up: the opening grant covers the first day, so the
                // first top-up falls due a day after the account was made.
                .orElseGet(() -> ledgerRepository.latestOf(uid, LedgerReason.OPENING_GRANT.name())
                        .map(opening -> now - opening.created_at() >= ONE_DAY_MILLIS)
                        .orElse(true));

        if (due) {
            record(uid, DAILY_TOPUP, LedgerReason.DAILY_TOPUP, null, "Daily top-up");
        }
    }

    public List<LedgerEntry_> historyOf(Long uid) {
        return ledgerRepository.historyOf(uid, HISTORY_LIMIT);
    }
}
