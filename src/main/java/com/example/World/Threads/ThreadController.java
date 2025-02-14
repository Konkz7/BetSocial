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

    public ThreadController(ThreadRepository threadRepository, BetRepository betRepository) {
        this.threadRepository = threadRepository;
        this.betRepository = betRepository;
    }

    @GetMapping("/all")
    List<Thread_>findAll(){
        return threadRepository.findAll();
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
        Long tid = threadRepository.save(new Thread_(null,uid,thread.title(), thread.description(), thread.category(),
                new Date().getTime(),null,thread.is_private(),null)).tid();

        return ResponseEntity.ok(String.valueOf(tid));
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/remove/{tid}")
    void removeThread(@PathVariable Long tid,HttpSession session){

        Long userId = (Long) session.getAttribute("userId");
        Optional<Thread_> optionalThread = threadRepository.findById(tid);
        Thread_ thread;
        if(optionalThread.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found");
        }else{
            thread = optionalThread.get();
        }

        if(!thread.uid().equals(userId)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the owner of this thread");
        }


        threadRepository.remove(tid, new Date().getTime());

        betRepository.findByThread(tid).forEach(bet -> {
            betRepository.updateStatus(bet.bid(), Status.CANCELLED.toInt());
            betRepository.remove(bet.bid(), new Date().getTime());
        });

    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/delete/{tid}")
    void delete(@PathVariable Long tid){
        threadRepository.delete(threadRepository.findById(tid).get());
    }


}
