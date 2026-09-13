package com.example.World.Users;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.World.Blocks.BlockService;
import com.example.World.Media.MediaReference;
import com.example.World.RateLimit.Limits;
import com.example.World.RateLimit.RateLimiter;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import com.example.World.Bets.DecisionDTO;
import com.example.World.Users.User_;
import com.example.World.Users.UserRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RequestMapping("/api/users")
@RestController
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final UserRepository userRepository;
    private final UserService userService;
    private final BlockService blockService;
    private final AccountDataService accountDataService;
    private final MediaReference mediaReference;
    private final DownloadTokens downloadTokens;
    private final RateLimiter rateLimiter;

    public UserController(UserRepository userRepository, UserService userService,
                          BlockService blockService, AccountDataService accountDataService,
                          MediaReference mediaReference, DownloadTokens downloadTokens,
                          RateLimiter rateLimiter) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.blockService = blockService;
        this.accountDataService = accountDataService;
        this.mediaReference = mediaReference;
        this.downloadTokens = downloadTokens;
        this.rateLimiter = rateLimiter;
    }

    /**
     * The caller's id, or 401.
     *
     * The rest of this controller reads the attribute inline and trusts it. The
     * two endpoints below cannot: one puts the id straight into a query as the
     * viewer whose blocks apply, and a null there would silently match nobody's
     * blocks rather than refusing the request.
     */
    private static Long requireUserId(HttpSession session){
        Long uid = (Long) session.getAttribute("userId");
        if(uid == null){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        return uid;
    }

    /** How many people a search returns. Enough to choose from, not enough to scroll. */
    private static final int SEARCH_LIMIT = 30;

    /** The most ids one request will resolve. A screenful of notifications, generously. */
    private static final int MAX_IDS = 200;

    // GET /all is gone. It returned every account in the database on every open
    // of the search screen, the group-creation picker and the member list. All
    // three were choosing somebody and all three filtered by name in memory
    // afterwards, so the list was both unbounded and larger than anything any of
    // them showed.

    /**
     * People matching a name.
     *
     * Capped, and the filtering the three calling screens each did in memory now
     * happens once, in the database. An empty term returns the first page
     * alphabetically so a picker opens with something in it.
     */
    @GetMapping("/search")
    List<UserView> search(@RequestParam(required = false) String q, HttpSession session){
        Long uid = requireUserId(session);
        String term = q == null ? "" : q.trim();
        return userRepository.search(uid, term, SEARCH_LIMIT).stream()
                .map(UserView::from)
                .toList();
    }

    /**
     * Specific accounts by id, for screens that hold ids and need names.
     *
     * The activity list used to fetch every account and search it in memory. That
     * worked only because the list was everything; capped, it would have failed
     * to name anybody outside the first page - and failed quietly, which is
     * worse.
     */
    @GetMapping("/by-ids")
    List<UserView> byIds(@RequestParam List<Long> ids, HttpSession session){
        requireUserId(session);

        if (ids.isEmpty()) {
            return List.of();
        }
        if (ids.size() > MAX_IDS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Too many ids in one request; the limit is " + MAX_IDS);
        }
        return userRepository.findAllByIds(ids).stream().map(UserView::from).toList();
    }

    @GetMapping("/{uid}")
    UserView findById(@PathVariable Long uid, HttpSession session){
        Optional<User_> user = userRepository.findById(uid);
        if(user.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        // Same answer as a genuinely missing user, so a profile cannot be reached
        // by id once either party has blocked the other.
        blockService.requireNotBlocked((Long) session.getAttribute("userId"), uid);
        return UserView.from(user.get());
    }

    @PutMapping("/change-bio")
    ResponseEntity<String> changeBio(@RequestParam String bio, HttpSession session){
        Long uid = (Long) session.getAttribute("userId");
        if(bio.length() > 380){
            return ResponseEntity.badRequest().body("This bio has too many characters");
        }

        userRepository.changeBio(uid,bio);

        return ResponseEntity.ok().body("Bio successfully changed!");
    }

    @PutMapping("/change-pfp")
    ResponseEntity<String> changeProfilePicture(@RequestParam String pfp, HttpSession session){
        Long uid = (Long) session.getAttribute("userId");

        // A profile picture is rendered against every thread, comment and message
        // that person has ever written, so an unchecked URL here is fetched by
        // more people than any other piece of media in the app.
        log.debug("User {} changed profile picture", uid);
        userRepository.changeProfilePicture(uid, mediaReference.require(pfp));


        return ResponseEntity.ok().body("Profile picture changed!");

    }

    @PutMapping("change-user-name")
    ResponseEntity<String> changeUserName(@RequestParam String newName, HttpSession session){
        Long uid = (Long) session.getAttribute("userId");
        if(newName.length() > 50){
            return ResponseEntity.badRequest().body("This name has too many characters");
        }

        for(User_ user : userRepository.findAll()){
            if(newName.equals(user.user_name())){
                return ResponseEntity.badRequest().body("This name already exists");
            }
        }
        userRepository.changeUserName(uid,newName);

        return ResponseEntity.ok().body("Name successfully changed!");
    }


    @PutMapping("/save-FBNtoken")
    ResponseEntity<String> refreshFBN(@RequestParam String FBNtoken, HttpSession session){
        Long uid = (Long) session.getAttribute("userId");

        log.debug("User {} registered a push token", uid);

        return userService.uniqueFBNLog(uid,FBNtoken) ? ResponseEntity.ok().body("FBN changed!") :
                ResponseEntity.badRequest().body("FBN couldnt be saved");

    }




    /**
     * A copy of everything held about the caller. UK GDPR, right of access.
     *
     * Their own only - there is no parameter for whose data to fetch, because
     * that is the one mistake this endpoint could make that would matter.
     */
    @GetMapping("/my-data")
    Map<String, Object> myData(HttpSession session){
        return accountDataService.export(requireUserId(session));
    }

    /**
     * A link the phone's browser can open to save the export as a file.
     *
     * The app cannot write somewhere its owner can find afterwards: from Android
     * 10 the public Downloads folder is closed to ordinary file writes, and
     * React Native's share sheet takes a string rather than a file on Android.
     * The browser can, so the export is handed to it - which means the URL has
     * to carry its own permission, because the browser has no session cookie.
     * See DownloadTokens for why that is safe to do and for how briefly.
     */
    @PostMapping("/my-data/link")
    Map<String, Object> dataDownloadLink(HttpSession session){
        Long uid = requireUserId(session);
        rateLimiter.require(RateLimiter.scopeOf("data-export", uid), Limits.DATA_EXPORT);

        log.info("Issued a data download link for user {}", uid);
        return Map.of(
                "token", downloadTokens.issue(uid),
                "expiresInSeconds", DownloadTokens.LIFETIME.toSeconds());
    }

    /**
     * The export itself, as a file, for whoever holds a valid token.
     *
     * Unauthenticated by necessity - see above - so the token is the whole of
     * the authorisation, and the user it names is the only one whose data this
     * can return. There is deliberately no parameter for whose data to fetch.
     */
    @GetMapping("/my-data/download")
    ResponseEntity<Map<String, Object>> downloadMyData(@RequestParam String token){
        long uid = downloadTokens.consume(token);

        String filename = "betsocial-my-data-" + LocalDate.now() + ".json";
        log.info("Data export downloaded for user {}", uid);

        return ResponseEntity.ok()
                // What makes the browser save it rather than render it, and what
                // gives the saved file a name somebody can recognise later.
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(accountDataService.export(uid));
    }

    /**
     * Deletes the caller's account. UK GDPR, right to erasure.
     *
     * The password is required in the body: this cannot be undone, and a session
     * is easier to come by than a password.
     *
     * What this replaces hard-deleted the row. Almost every table referencing
     * user_ cascades, so it took their threads, comments, messages, predictions,
     * follows and ledger entries with it - and failed outright for anybody who
     * had ever decided a bet outcome, because decision_log does not cascade.
     */
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/delete")
    void delete(@RequestBody DeleteAccountDTO confirmation, HttpSession session){
        accountDataService.deleteAccount(requireUserId(session), confirmation.pass_word());
        session.invalidate();
    }
}

