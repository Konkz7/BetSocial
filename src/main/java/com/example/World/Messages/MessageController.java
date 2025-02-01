package com.example.World.Messages;



import com.example.World.Threads.ThreadDTO;
import com.example.World.Threads.Thread_;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequestMapping("/api/messages")
@RestController
public class MessageController {
    private final MessageRepository messageRepository;
    private final MessageService messageService;

    public MessageController(MessageRepository messageRepository, MessageService messageService) {
        this.messageRepository = messageRepository;
        this.messageService = messageService;
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

    @MessageMapping("/send/{gid}")
    @SendTo("/topic/chat/{gid}")
    public Message_ sendMessage(@PathVariable Long gid, @Valid @RequestBody MessageDTO message, HttpSession session) {
        Long uid = (Long) session.getAttribute("userId");
        return messageService.sendMessage(gid, uid, message.recipient_id(), message.description());
    }

    //unnecessary****************
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
