package com.example.World.Users;


import com.example.World.Bets.DecisionDTO;
import com.example.World.Users.User_;
import com.example.World.Users.UserRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
    List<User_> findAll(){
        return userRepository.findAll();
    }

    @GetMapping("/{uid}")
    User_ findById(@PathVariable Long uid){
        Optional<User_> user = userRepository.findById(uid);
        if(user.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        return user.get();
    }




    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/delete")
    void delete(HttpSession session){
        Long userId = (Long) session.getAttribute("userId");
        userRepository.delete(userRepository.findById(userId).get());
    }
}

