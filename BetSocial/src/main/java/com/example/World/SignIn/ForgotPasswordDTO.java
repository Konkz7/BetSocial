package com.example.World.SignIn;

import jakarta.validation.constraints.NotEmpty;

/**
 * Who to send a reset link to - a username or an email address.
 *
 * @Email used to be asserted here, which made a username a 400 before the
 * service ever saw it. The field it is typed into is the login screen's, and
 * that accepts either, so validating one of the two shapes rejected half of
 * what the screen invites. The link itself is always sent to the account's own
 * address, so nothing is emailed anywhere a person typed.
 */
public record ForgotPasswordDTO(

        @NotEmpty
        String account
) {
}
