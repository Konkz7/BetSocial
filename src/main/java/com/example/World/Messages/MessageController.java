package com.example.World.Messages;



import com.example.World.Threads.ThreadDTO;
import com.example.World.Threads.Thread_;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


import java.util.List;
import java.util.Optional;

@RequestMapping("/api/messages")
@RestController
public class MessageController {
    private final MessageRepository messageRepository;
    private final MessageService messageService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public MessageController(MessageRepository messageRepository, MessageService messageService,
                             SimpMessagingTemplate simpMessagingTemplate) {
        this.messageRepository = messageRepository;
        this.messageService = messageService;
        this.simpMessagingTemplate = simpMessagingTemplate;
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

    @MessageMapping("/send")
    public Message_ sendMessage(@Payload MessageDTO message, SimpMessageHeaderAccessor headerAccessor) {

        Object uidObj = headerAccessor.getSessionAttributes().get("userId");
        if (uidObj == null) {
            throw new IllegalStateException("User ID not found in session");
        }

        Long uid = (Long) uidObj;
        Long gid = message.gid();
        System.out.println("MESSAGE: " + message.description());
        Message_ result = messageService.sendMessage(gid, uid, message.recipient_id(), message.description());

        simpMessagingTemplate.convertAndSend("/topic/chat/" + gid, result);

        return result;
    }

    @MessageMapping("/add-user/{gid}")
    @SendTo("/topic/chat/{gid}")
    public GroupStatus addUser(SimpMessageHeaderAccessor headerAccessor,HttpSession session) {
        Long uid = (Long) session.getAttribute("userId");
        headerAccessor.getSessionAttributes().put("userId",uid);
        return new GroupStatus(uid,true); 
    }

    @GetMapping("/group/{gid}")
    List<Message_> getGroupMessages(@PathVariable Long gid){
        return messageService.getChatMessages(gid);
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
