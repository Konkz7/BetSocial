package com.example.World.Threads;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequestMapping("/api/threads")
@RestController
public class ThreadController {


    private final ThreadRepository threadRepository;

    public ThreadController(ThreadRepository threadRepository) {
        this.threadRepository = threadRepository;
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
    void makeThread(@Valid @RequestBody ThreadDTO thread, HttpSession session){
        Long uid = (Long) session.getAttribute("userId");
        threadRepository.save(new Thread_(null,uid,thread.title(), thread.description(), thread.category(), LocalDateTime.now(),null,null));
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/delete/{tid}")
    void delete(@PathVariable Long tid){
        threadRepository.delete(threadRepository.findById(tid).get());
    }


}
