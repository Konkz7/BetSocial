package com.example.World.SignIn;

import jakarta.validation.constraints.NotEmpty;

/**
 * A reset link and the password to set.
 *
 * The length rule lives in PasswordResetService rather than as an annotation
 * here, so that the message matches the one registration gives and there is one
 * place to change it.
 */
public record ResetPasswordDTO(

        @NotEmpty
        String token,

        @NotEmpty
        String new_password
) {
}
