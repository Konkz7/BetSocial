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

    public CommentController(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
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
    void makeComment(@Valid @RequestBody CommentDTO comment, HttpSession session){
        Long uid = (Long) session.getAttribute("userId");
        commentRepository.save(new Comment_(null, comment.tid(), uid, comment.parent_cid(), comment.description(),new Date().getTime(),null,null));
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
