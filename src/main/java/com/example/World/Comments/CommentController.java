package com.example.World.Comments;


import com.example.World.Comments.Comment_;
import com.example.World.Comments.CommentRepository;
import com.example.World.Threads.ThreadDTO;
import com.example.World.Threads.Thread_;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


import java.util.Date;
import java.util.List;
import java.util.Optional;

@RequestMapping("/api/comments")
@RestController
public class CommentController {
    private final CommentRepository commentRepository;
    private final CommentService commentService;

    public CommentController(CommentRepository commentRepository, CommentService commentService) {
        this.commentRepository = commentRepository;
        this.commentService = commentService;
    }

    @GetMapping("/all")
    List<Comment_> findAll(){
        return commentRepository.findAll();
    }

    @GetMapping("/{cid}")
    Comment_ findById(@PathVariable Long cid){
        Optional<Comment_> conversation = commentRepository.findById(cid);
        if(conversation.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "conversation not found");
        }
        return conversation.get();
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/create")
    void create(@Valid @RequestBody Comment_ comment){
        commentRepository.save(comment);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/make")
    Comment_ makeComment(@Valid @RequestBody CommentDTO comment, HttpSession session){
        Long uid = (Long) session.getAttribute("userId");
        return commentService.makeComment(comment,uid);
    }


    @GetMapping("/get-by-thread/{tid}")
    List<CommentProfile> getComments(@PathVariable Long tid,HttpSession session){
        Long uid = (Long) session.getAttribute("userId");
        return commentService.getComments(uid,tid);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/register-like")
    void registerCommentLike(@RequestParam Long tid, @RequestParam Long cid , @RequestParam boolean liked,HttpSession session){
        Long userId = (Long) session.getAttribute("userId");
        commentService.registerCommentLike(userId,cid ,tid, liked);
    }

    @GetMapping("/comment-likes/{tid}")
    List<Commentlike_> getCommentLikes(@PathVariable Long tid,HttpSession session){
        Long uid = (Long) session.getAttribute("userId");
        return commentService.getCommentLikes(uid,tid);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/delete/{cid}")
    void deleteComment(@PathVariable Long cid){
        try {
            commentService.deleteComment(cid);
        }catch (Exception e){
        }
    }
/*
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/update/{cid}")
    void update(@Valid @RequestBody Conversation conversation, @PathVariable Integer cid){
        conversationRepository.updateConversation(cid, conversation.result(), conversation.amount(), conversation.status());
    }

 */

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/delete/{cid}")
    void delete(@PathVariable Long cid){
        commentRepository.delete(commentRepository.findById(cid).get());
    }
}
