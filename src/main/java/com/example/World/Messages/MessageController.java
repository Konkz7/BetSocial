package com.example.World.Messages;



import com.example.World.Threads.ThreadDTO;
import com.example.World.Threads.Thread_;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequestMapping("/api/messages")
@RestController
public class MessageController {
    private final MessageRepository messageRepository;

    public MessageController(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @GetMapping("/all")
    List<Message_> findAll(){
        return messageRepository.findAll();
    }

    @GetMapping("/{mid}")
    Message_ findById(@PathVariable Long mid){
        Optional<Message_> message = messageRepository.findById(mid);
        if(message.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "message not found");
        }
        return message.get();
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/create")
    void create(@Valid @RequestBody Message_ message){
        messageRepository.save(message);
    }



    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/delete/{mid}")
    void delete(@PathVariable Long mid){
        messageRepository.delete(messageRepository.findById(mid).get());
    }
}
