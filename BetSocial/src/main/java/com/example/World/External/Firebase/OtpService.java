package com.example.World.External.Firebase;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import org.springframework.stereotype.Service;

@Service
public class OtpService {
    public String sendOtp(String phoneNumber) throws FirebaseAuthException {
        return FirebaseAuth.getInstance()
                .createCustomToken(phoneNumber);
    }


}



