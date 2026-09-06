package com.example.World.Messages;



import com.example.World.Notifications.NotificationService;
import com.example.World.Security.WebSocketPrincipal;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private final NotificationService notificationService;


    public MessageController(MessageRepository messageRepository, MessageService messageService,
                             SimpMessagingTemplate simpMessagingTemplate, NotificationService notificationService) {
        this.messageRepository = messageRepository;
        this.messageService = messageService;
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.notificationService = notificationService;
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

        // The sender is the authenticated principal from the handshake, never a
        // value supplied by the client.
        Long uid = WebSocketPrincipal.requireUserId(headerAccessor.getUser());
        Long gid = message.gid();

        // A member check here as well as on CONNECT: a client may publish to any
        // gid it likes once connected, regardless of what it declared at connect.
        messageService.requireMembership(gid, uid);

        Message_ result = messageService.sendMessage(gid, uid, message.recipient_id(), message.description(), message.media_type());

        simpMessagingTemplate.convertAndSend("/topic/chat/" + gid, result);

        return result;
    }

    /*
    @MessageMapping("/add-user/{gid}")
    @SendTo("/topic/chat/{gid}")
    public GroupStatus addUser(SimpMessageHeaderAccessor headerAccessor,HttpSession session) {
        Long uid = (Long) session.getAttribute("userId");
        headerAccessor.getSessionAttributes().put("userId",uid);
        return new GroupStatus(uid,true); 
    }
    
     */

    // Previously unchecked: any authenticated user could read any conversation.
    @GetMapping("/group/{gid}")
    List<Message_> getGroupMessages(@PathVariable Long gid, HttpSession session){
        return messageService.getChatMessages(gid, requireUserId(session));
    }

    @GetMapping("/conversations")
    List<ConversationDTO> getGroupMessages(HttpSession session){
        Long uid = (Long) session.getAttribute("userId");

        return messageService.getConversations(uid);
    }


    @PutMapping("/update-reads/{gid}")
    void updatePrevReadReceipts(@PathVariable Long gid, HttpSession session){
        Long uid = requireUserId(session);
        messageService.requireMembership(gid, uid);
        messageService.updatePrevReadReceipts(uid,gid);
    }

    // Previously unchecked: any authenticated user could delete anyone's message.
    @PutMapping("/delete")
    void softDeleteMessage(@RequestParam Long mid, @RequestParam Long gid, HttpSession session){
        messageService.deleteMessage(mid, requireUserId(session));

        simpMessagingTemplate.convertAndSend("/topic/chat/" + gid, new MessageAction("DELETE" , mid , null));
    }

    private static Long requireUserId(HttpSession session){
        Long uid = (Long) session.getAttribute("userId");
        if(uid == null){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        return uid;
    }

    @PutMapping("/update-read/{mid}")
    void updateReadReceipt(@PathVariable Long mid){
        messageRepository.updateReadReceipt(mid);
    }
    void delete(@PathVariable Long mid){
        messageRepository.delete(messageRepository.findById(mid).get());
    }
}
