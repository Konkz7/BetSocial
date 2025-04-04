package com.example.World.Users;


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

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/all")
    List<User_> findAll(HttpSession session){
        Long uid = (Long) session.getAttribute("userId");
        return userRepository.findAllActiveUsers(uid);
    }

    @GetMapping("/{uid}")
    User_ findById(@PathVariable Long uid){
        Optional<User_> user = userRepository.findById(uid);
        if(user.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        return user.get();
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




    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/delete")
    void delete(HttpSession session){
        Long userId = (Long) session.getAttribute("userId");
        userRepository.delete(userRepository.findById(userId).get());
    }
}

