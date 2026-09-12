package com.example.World.Users;


import com.example.World.Blocks.BlockService;
import java.util.Set;
import com.example.World.Bets.DecisionDTO;
import com.example.World.Users.User_;
import com.example.World.Users.UserRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RequestMapping("/api/users")
@RestController
public class UserController {
    private final UserRepository userRepository;
    private final UserService userService;
    private final BlockService blockService;

    public UserController(UserRepository userRepository, UserService userService,
                          BlockService blockService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.blockService = blockService;
    }

    @GetMapping("/all")
    List<UserView> findAll(HttpSession session){
        Long uid = (Long) session.getAttribute("userId");
        Set<Long> invisible = blockService.invisibleTo(uid);
        return userRepository.findAllActiveUsers(uid).stream()
                .filter(u -> !invisible.contains(u.uid()))
                .map(UserView::from)
                .toList();
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

        System.out.println(pfp);
        userRepository.changeProfilePicture(uid,pfp);


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

        System.out.println("DONE");

        return userService.uniqueFBNLog(uid,FBNtoken) ? ResponseEntity.ok().body("FBN changed!") :
                ResponseEntity.badRequest().body("FBN couldnt be saved");

    }




    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/delete")
    void delete(HttpSession session){
        Long userId = (Long) session.getAttribute("userId");
        userRepository.delete(userRepository.findById(userId).get());
    }
}

