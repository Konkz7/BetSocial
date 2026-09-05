package com.example.World.SignIn;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DetailDTO(

        @NotEmpty
        @Size(max = 50)
        String user_name,

        @NotEmpty
        @Size(min = 8)
        String pass_word, // pass_word (should be hashed before saving)

        @NotEmpty
        @Email
        @Size(max = 100)
        String email, // Unique email address, validated as a proper email format

        @NotEmpty
        @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
        String phone_number
) {
}
