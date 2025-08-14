package com.example.World.Threads;

import com.example.World.Bets.BetRepository;
import com.example.World.Bets.Status;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


import java.util.Date;
import java.util.List;
import java.util.Optional;

@RequestMapping("/api/threads")
@RestController
public class ThreadController {


    private final ThreadRepository threadRepository;
    private final BetRepository betRepository;
    private final ThreadService threadService;

    public ThreadController(ThreadRepository threadRepository, BetRepository betRepository, ThreadService threadService) {
        this.threadRepository = threadRepository;
        this.betRepository = betRepository;
        this.threadService = threadService;
    }

    @GetMapping("/all")
    List<Thread_>findAll(){
        return threadRepository.findAll();
    }

    @GetMapping("/active")
    List<Thread_>findAllActive(){
        return threadRepository.findAllActiveThreads();
    }

    @GetMapping("/user/{uid}")
    List<Thread_> findAllByUID(@PathVariable Long uid){
        return threadRepository.findAllUserThreads(uid);
    }

    @GetMapping("/{tid}")
    Thread_ findById(@PathVariable Long tid){
        Optional<Thread_> thread = threadRepository.findById(tid);
        if(thread.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found");
        }
        return thread.get();
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/create")
    void create(@Valid @RequestBody Thread_ thread){
        threadRepository.save(thread);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/make")
    ResponseEntity<String> makeThread(@Valid @RequestBody ThreadDTO thread, HttpSession session , BindingResult result){

        if(result.hasErrors()){
            return ResponseEntity.badRequest().body("Error: Please make sure fields are filled out properly");
        }
        Long uid = (Long) session.getAttribute("userId");
        Long tid = threadRepository.save(new Thread_(null,uid,thread.title(), thread.media(), thread.media_type(), thread.category(),
                new Date().getTime(),null,thread.is_private(),null)).tid();

        return ResponseEntity.ok(String.valueOf(tid));
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/remove/{tid}")
    void removeThread(@PathVariable Long tid,HttpSession session){

        Long userId = (Long) session.getAttribute("userId");
        threadService.removeThread(tid,userId);

    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/delete/{tid}")
    void delete(@PathVariable Long tid){
        threadRepository.delete(threadRepository.findById(tid).get());
    }


}
