package com.example.World.External.Firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.auth.FirebaseAuthException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    public String sendPasswordResetEmail(String email) throws FirebaseAuthException {
        return FirebaseAuth.getInstance().generatePasswordResetLink(email);
    }

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
