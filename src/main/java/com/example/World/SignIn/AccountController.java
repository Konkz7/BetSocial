package com.example.World.SignIn;

import com.example.World.Bets.Status;
import com.example.World.Firebase.AuthService;
import com.example.World.Users.UserDTO;
import com.example.World.Users.UserRepository;
import com.example.World.Users.UserRole;
import com.example.World.Users.User_;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static com.example.World.Users.UserRole.USER;

@RestController
@RequestMapping("/req")
public class AccountController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    //private final OtpService otpService;

    public AccountController(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthService authService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

    /**
     * Endpoint to register a new user.
     * Accepts POST requests and validates the input.
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody User_ user, HttpSession session) {

        /*
        if (userRepository.existsByEmail(user.email())) {
            return ResponseEntity.badRequest().body("Email is already in use.");
        }

         */
        if(session.getAttribute("userId") != null){
            return ResponseEntity.badRequest().body("Logout first to register as a new user.");
        } else if (userRepository.findByUsername(user.
                user_name()).isPresent()) {
            return ResponseEntity.badRequest().body("Username is already in use.");
        }



        // Hash the password
        String hashedPassword = passwordEncoder.encode(user.pass_word());

        // Create a new User_ instance with the hashed password
        User_ userWithHashedPassword = new User_(
                null,                 // Retain the original UID (if null, it's auto-generated)
                user.user_name(),           // Retain the username
                user.email(),               // Retain the email
                hashedPassword,             // Use the hashed password
                user.phone_number(),        // Retain the phone number
                LocalDateTime.now(),          // Retain the creation timestamp
                null,
                USER.toInt(),          // Retain the user role
                null            // Retain the version for optimistic locking
        );

        userRepository.save(userWithHashedPassword);
        return ResponseEntity.ok("User registered successfully!");
    }

    @PostMapping("/send-email-verification")
    public String sendVerificationEmail(@RequestParam String email) throws Exception {
        return authService.sendEmailVerification(email);
    }

    @GetMapping("/check-email-verification")
    public boolean checkVerificationEmail(@RequestParam String email) throws Exception {
        return authService.checkEmailVerified(email);
    }
/*
    @PostMapping("/send-otp")
    public String sendOtp(@RequestParam String phoneNumber) throws Exception {
        return otpService.sendOtp(phoneNumber);
    }

    @PostMapping("/verify-otp")
    public boolean verifyOtp(@RequestParam String otp, @RequestParam String verificationId) {
        return otpService.verifyOtp(otp, verificationId);
    }

 */

    @GetMapping("/profile")
    public ResponseEntity<UserDTO> getProfile(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not logged in");
        }

        User_ user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return ResponseEntity.ok(new UserDTO(user.uid(), user.user_name(), user.email() , user.phone_number(), user.created_at()));
    }


    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No user is logged in.");
        }

        // Invalidate the session if a user is logged in
        session.invalidate();
        return ResponseEntity.ok("Logged out successfully!");
    }
}
