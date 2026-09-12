package com.example.World.Users;

import jakarta.validation.constraints.NotEmpty;

/**
 * Confirmation for deleting an account.
 *
 * A body with a password rather than a bare DELETE, because the action cannot be
 * undone and a session is easier to come by than a password - a borrowed phone
 * should not be enough to delete somebody's account.
 */
public record DeleteAccountDTO(

        @NotEmpty
        String pass_word
) {
}
