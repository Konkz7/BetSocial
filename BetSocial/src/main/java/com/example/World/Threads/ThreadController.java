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

    // GET /all and GET /{tid} are deliberately absent. Both returned raw rows
    // straight from the repository, so neither applied the privacy rule nor - now
    // - blocking: /all handed every thread in the database, private ones
    // included, to any authenticated caller, and /{tid} did the same one id at a
    // time. Adding a block filter to the feed while leaving these in place would
    // have made blocking look implemented rather than be implemented.
    //
    // Neither had a client caller. /active and /thread-profile/{tid} are the
    // filtered equivalents and are what the app actually uses.

    /**
     * One page of the feed, newest first.
     *
     * Both cursor parameters come from the previous response's
     * next_cursor_created_at and next_cursor_tid; omit them for the first page.
     * They are two values rather than one opaque string because the client has no
     * need to treat them as opaque and a readable cursor is a readable log line.
     */
    @GetMapping("/active")
    FeedPage findAllActive(HttpSession session,
                           @RequestParam(required = false) Long cursor_created_at,
                           @RequestParam(required = false) Long cursor_tid){
        Long uid = (Long) session.getAttribute("userId");
        return threadService.feedPage(uid, cursor_created_at, cursor_tid);
    }

    @GetMapping("/user/{other_uid}")
    List<ThreadProfile> findAllByUID(@PathVariable Long other_uid,HttpSession session){
        Long uid = (Long) session.getAttribute("userId");
        return threadService.threadProfileList(uid,other_uid);
    }

    @GetMapping("/thread-likes")
    List<Threadlike_> threadLike(HttpSession session){
        Long uid = (Long) session.getAttribute("userId");
        return threadService.getUserThreadLikes(uid);
    }

    @GetMapping("/{tid}")
    Thread_ findById(@PathVariable Long tid){
        Optional<Thread_> thread = threadRepository.findById(tid);
        if(thread.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found");
        }
        return thread.get();
    }

    @GetMapping("thread-profile/{tid}")
    ThreadProfile toThreadProfile(@PathVariable Long tid, HttpSession session){
        Long uid = (Long) session.getAttribute("userId");
        return threadService.toThreadProfile(tid, uid);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/make")
    ResponseEntity<String> makeThread(@Valid @RequestBody ThreadDTO thread, HttpSession session , BindingResult result){

        if(result.hasErrors()){
            return ResponseEntity.badRequest().body("Error: Please make sure fields are filled out properly");
        }

        Long userId = (Long) session.getAttribute("userId");

        Thread_ temp = threadService.makeThread(thread, userId);


        return ResponseEntity.ok(String.valueOf(temp.tid()));
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/register-like")
    void registerThreadLike(@RequestParam Long tid , @RequestParam boolean liked,HttpSession session){
        Long userId = (Long) session.getAttribute("userId");
        threadService.registerThreadLike(userId,tid, liked);
    }


    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/remove/{tid}")
    void removeThread(@PathVariable Long tid,HttpSession session){

        Long userId = (Long) session.getAttribute("userId");
        threadService.removeThread(tid,userId);

    }


}
