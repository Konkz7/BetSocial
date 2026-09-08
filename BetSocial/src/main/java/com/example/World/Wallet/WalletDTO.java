package com.example.World.Wallet;

import java.util.List;

/**
 * The whole wallet screen: a balance and the movements that add up to it.
 *
 * Sent together because they are one thing. A balance on its own is a number to
 * be taken on trust; with the entries beside it, it can be checked.
 */
public record WalletDTO(

        long balance,
        List<LedgerEntry_> entries
) {
}
