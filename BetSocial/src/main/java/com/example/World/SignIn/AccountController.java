package com.example.World.SignIn;

import com.example.World.RateLimit.Limits;
import com.example.World.RateLimit.RateLimiter;
import com.example.World.External.Emails.EmailService;
import com.example.World.External.Firebase.AuthService;
import com.example.World.Users.*;
import com.example.World.Wallet.LedgerService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


import java.util.Date;
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
    private final LedgerService ledgerService;
    private final RateLimiter rateLimiter;
    private final PasswordResetService passwordResetService;


    public AccountController(PasswordEncoder passwordEncoder, UserRepository userRepository, AuthService authService, EmailService emailService, LedgerService ledgerService,
                             RateLimiter rateLimiter, PasswordResetService passwordResetService) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.authService = authService;
        this.emailService = emailService;
        this.ledgerService = ledgerService;
        this.rateLimiter = rateLimiter;
        this.passwordResetService = passwordResetService;
    }


    /**
     * Asks for a reset link. Always answers the same way.
     *
     * Whether that address has an account is not in the response, because
     * answering would turn this into a way to ask whether somebody is a user -
     * worth more to whoever is asking than to the person who mistyped their own
     * address.
     *
     * Rate limited by address rather than account, since there is no session
     * here: without it this is an open relay for sending mail to any address
     * somebody likes.
     */
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/forgot-password")
    public String forgotPassword(@Valid @RequestBody ForgotPasswordDTO request,
                                 HttpServletRequest servletRequest) {
        rateLimiter.require(RateLimiter.scopeOf("forgot-password", servletRequest.getRemoteAddr()),
                Limits.FORGOT_PASSWORD);

        passwordResetService.requestReset(request.email());
        return "If that address has an account, a reset link is on its way.";
    }

    /**
     * The page the emailed link lands on.
     *
     * A link in an email is opened by a browser, and the endpoint below only
     * answers POST - so without this, tapping the link gives a 405 and the
     * feature does not work at all. The alternative is a deep link into the app,
     * which needs native URL-scheme configuration on both platforms and still
     * fails for anybody reading their email on a laptop.
     *
     * Deliberately plain and self-contained: no stylesheet, no framework, one
     * field. The token is written into a script constant and posted as JSON, so
     * there is one POST endpoint rather than two.
     */
    @GetMapping(value = "/reset-password", produces = MediaType.TEXT_HTML_VALUE)
    public String resetPasswordPage(@RequestParam("token") String token) {
        // Rejected outright rather than escaped. This value is reflected into a
        // <script> block, where HTML escaping does nothing useful - entities are
        // not decoded there, so htmlEscape would neither protect the page nor
        // survive a legitimate token. Tokens are Base64url by construction, so
        // anything outside that alphabet did not come from us and there is
        // nothing to render for it.
        if (token == null || !token.matches("[A-Za-z0-9_-]{1,256}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "That is not a reset link");
        }

        return """
        <!doctype html>
        <html lang="en"><head><meta charset="utf-8">
        <meta name="viewport" content="width=device-width,initial-scale=1">
        <title>Reset your password</title></head>
        <body style="font-family:system-ui,sans-serif;max-width:24rem;margin:3rem auto;padding:0 1rem">
          <h1 style="font-size:1.25rem">Reset your password</h1>
          <p id="msg" style="color:#6b7280"></p>
          <input id="pw" type="password" placeholder="New password" autocomplete="new-password"
                 style="width:100%%;padding:.6rem;font-size:1rem;box-sizing:border-box">
          <button id="go" style="width:100%%;padding:.7rem;margin-top:.6rem;font-size:1rem;
                 background:#10B981;color:#fff;border:0;border-radius:.4rem">Set password</button>
          <script>
            const token = "%s";
            document.getElementById('go').onclick = async () => {
              const msg = document.getElementById('msg');
              msg.textContent = 'Working...';
              const res = await fetch('/req/reset-password', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({token: token, new_password: document.getElementById('pw').value})
              });
              const text = await res.text();
              msg.textContent = res.ok ? 'Done. Open the app and sign in.' : text;
            };
          </script>
        </body></html>
        """.formatted(token);
    }

    /**
     * Sets a new password from a reset link.
     *
     * Rate limited as well: the token is 32 random bytes, so guessing is not a
     * realistic attack, but nothing here should be free to hammer.
     */
    @PostMapping("/reset-password")
    public String resetPassword(@Valid @RequestBody ResetPasswordDTO request,
                                HttpServletRequest servletRequest) {
        rateLimiter.require(RateLimiter.scopeOf("reset-password", servletRequest.getRemoteAddr()),
                Limits.RESET_PASSWORD);

        passwordResetService.resetPassword(request.token(), request.new_password());
        return "Your password has been changed. Sign in with it now.";
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
    public ResponseEntity<String> register(@Valid @RequestBody DetailDTO user, HttpSession session,
                                           HttpServletRequest request) {

        // Keyed by address, because there is no account yet to key on. That is
        // weaker than it looks - an address is shared by everyone behind one
        // router and changed freely by anyone determined - but it is the only
        // identity a registration has, and it stops the obvious script.
        rateLimiter.require(RateLimiter.scopeOf("register", request.getRemoteAddr()),
                Limits.REGISTER);

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
                "Hi, Im new here!",
                null,
                new Date().getTime(),          // Retain the creation timestamp
                null,
                USER.toInt(),          // Retain the user role
                null,
                "offline",
                null,              // wallet address
                0.0,     // balance
                null            // Retain the version for optimistic locking
        );

        emailService.sendVerificationEmail(userWithHashedPassword.email(), token);
        User_ saved = userRepository.save(userWithHashedPassword);

        // The opening grant, without which a new account cannot stake anything and
        // has no way to earn its first coin either.
        ledgerService.grantOpeningBalance(saved.uid());

        return ResponseEntity.ok("User registered successfully!");
    }

    /**
     * The caller's own profile.
     *
     * Returns ProfileView rather than UserView, which is the same fields plus the
     * caller's role. The client needs it to decide which interface to open after
     * signing in, and it stays off UserView so that which accounts are privileged
     * is not handed out with every profile, comment and member list.
     */
    @GetMapping("/profile")
    public ResponseEntity<ProfileView> getProfile(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not logged in");
        }

        User_ user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return ResponseEntity.ok(ProfileView.from(user));
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
