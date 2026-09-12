package com.example.World.SignIn;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;

/** An address to send a reset link to. */
public record ForgotPasswordDTO(

        @NotEmpty
        @Email
        String email
) {
}
