package com.example.World.SignIn;

import com.example.World.External.Emails.EmailService;
import com.example.World.External.Firebase.AuthService;
import com.example.World.Users.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.example.World.Users.UserRole.USER;

@RestController
@RequestMapping("/req")
@CrossOrigin("*")
public class AccountController {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private  final AuthService authService;
    private  final EmailService emailService;


    //private final OtpService otpService;

    public AccountController(PasswordEncoder passwordEncoder, UserRepository userRepository, AuthService authService, EmailService emailService) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.authService = authService;
        this.emailService = emailService;
    }


    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam("token") String token) {
        User_ user = userRepository.findByVerificationToken(token).orElseThrow();

        userRepository.verify(user.uid());
        return ResponseEntity.ok("Email verified successfully!");
    }


    @PostMapping("/phone-verification")
    public ResponseEntity<String> verifyPhoneToken(@RequestParam String idToken) throws FirebaseAuthException {
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
        try {
            String uid = decodedToken.getUid();
            return ResponseEntity.ok("User verified with UID: " + uid);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid token: " + e.getMessage());
        }
    }

    @PostMapping("/check-details")
    public ResponseEntity<String> checkDetails(@Valid @RequestBody DetailDTO details, BindingResult result) {

        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(result.getAllErrors().getFirst().getDefaultMessage());
        }

        if (userRepository.existsByEmail(details.email())) {
            return ResponseEntity.badRequest().body("Email is already in use.");
        }else if (userRepository.existsByPhoneNumber(details.phone_number())) {
            return ResponseEntity.badRequest().body("Phone number is already in use.");
        }else if (userRepository.existsByUserName(details.user_name())) {
            return ResponseEntity.badRequest().body("Username is already in use.");
        }

        if(details.pass_word().length() < 8){
            return ResponseEntity.badRequest().body("Password should be at least 8 characters long.");
        }

        return ResponseEntity.ok("Details are fine!");
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody DetailDTO user, HttpSession session) {

        /*
        if (userRepository.existsByEmail(user.email())) {
            return ResponseEntity.badRequest().body("Email is already in use.");
        }

         */
        if(session.getAttribute("userId") != null){
            return ResponseEntity.badRequest().body("Logout first to register as a new user.");
        }


        // Hash the password
        String hashedPassword = passwordEncoder.encode(user.pass_word());
        String token = UUID.randomUUID().toString();

        // Create a new User_ instance with the hashed password
        User_ userWithHashedPassword = new User_(
                null,                 // Retain the original UID (if null, it's auto-generated)
                user.user_name(),           // Retain the username
                user.email(),               // Retain the email
                hashedPassword,             // Use the hashed password
                user.phone_number(),        // Retain the phone number
                token,                       // Retain the verification token
                false,                      // Email is not verified
                LocalDateTime.now(),          // Retain the creation timestamp
                null,
                USER.toInt(),          // Retain the user role
                null            // Retain the version for optimistic locking
        );

        emailService.sendVerificationEmail(userWithHashedPassword.email(), token);
        userRepository.save(userWithHashedPassword);
        return ResponseEntity.ok("User registered successfully!");
    }

    @GetMapping("/profile")
    public ResponseEntity<UserDTO> getProfile(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not logged in");
        }

        User_ user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return ResponseEntity.ok(new UserDTO(userId, user.user_name(), user.email() , user.phone_number(), user.created_at()));
    }


    /**
     * Endpoint to register a new user.
     * Accepts POST requests and validates the input.
     */



/*
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No user is logged in.");
        }

        // Invalidate the session if a user is logged in
        session.invalidate();
        return ResponseEntity.ok("Logged out successfully!");
    }

 */
}
