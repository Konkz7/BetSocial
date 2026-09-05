package com.example.World.Users;


import com.example.World.Users.User_;
import com.example.World.Users.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RequestMapping("/admin")
@RestController
public class AdminController {
    private final UserRepository userRepository;

    public AdminController(UserRepository userRepository) {
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

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/create")
    void create(@Valid @RequestBody User_ user){
        userRepository.save(user);
    }
/*
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/update/{uid}")
    void update(@Valid @RequestBody User user, @PathVariable Integer uid){
        UserRepository.updateUser(uid, User.User(), User.amount());
    }

 */

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/delete/{uid}")
    void delete(@PathVariable Long uid){
        userRepository.delete(userRepository.findById(uid).get());
    }
}

