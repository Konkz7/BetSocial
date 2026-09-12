package com.example.World.External.Firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.auth.FirebaseAuthException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    // sendPasswordResetEmail is gone. It asked Firebase Auth to reset a Firebase
    // password, and sign-in does not go through Firebase - it goes through
    // DaoAuthenticationProvider against the BCrypt hash in user_. Anybody who
    // followed the link it produced would have set a new password and still been
    // locked out. It had never been called, which is the only reason nobody hit
    // that. PasswordResetService does this properly.

    public String sendEmailVerification(String email) throws FirebaseAuthException {
        UserRecord user = FirebaseAuth.getInstance().getUserByEmail(email);

        if (user.isEmailVerified()) {
            return "Email already verified.";
        }

        return FirebaseAuth.getInstance().generateEmailVerificationLink(email);
    }

    public boolean checkEmailVerified(String email) throws FirebaseAuthException {
        UserRecord user = FirebaseAuth.getInstance().getUserByEmail(email);
        return user.isEmailVerified();
    }


}
