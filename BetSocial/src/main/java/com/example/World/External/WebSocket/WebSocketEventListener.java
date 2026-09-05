package com.example.World.External.WebSocket;



import com.example.World.Messages.GroupStatus;
import com.example.World.Messages.Message_;
import com.example.World.Users.UserRepository;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Objects;

@Component
public class WebSocketEventListener {

    private final SimpMessageSendingOperations messageOperations;
    private final UserRepository userRepository;

    public WebSocketEventListener(SimpMessageSendingOperations messageOperations, UserRepository userRepository) {
        this.messageOperations = messageOperations;
        this.userRepository = userRepository;
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event){
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Long id = (Long) headerAccessor.getSessionAttributes().get("userId");
        Long gid = (Long) headerAccessor.getSessionAttributes().get("chatId");

        if (id != null && gid != null) {
            System.out.println("User " + id + " disconnected from chat " + gid);
            userRepository.changeStatus(id,"online");

            GroupStatus gp = new GroupStatus(id, gid, false , true);
            messageOperations.convertAndSend("/topic/chat/" + gid, gp);
        }
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        // Example: extract userId from headers or session
        // If you have custom auth, you'd typically pass userId via a header
        String userIdStr = headerAccessor.getFirstNativeHeader("userId");
        String chatIdStr = headerAccessor.getFirstNativeHeader("chatId");


        if (userIdStr != null && chatIdStr != null) {
            Long id = Long.parseLong(userIdStr);
            Long gid = Long.parseLong(chatIdStr);

            headerAccessor.getSessionAttributes().put("userId", id);
            headerAccessor.getSessionAttributes().put("chatId", gid);

            System.out.println("User " + id + " connected to chat " + gid);
            userRepository.changeStatus(id,"online/chat/" + gid);

            GroupStatus gp = new GroupStatus(id, gid, true , true);
            messageOperations.convertAndSend("/topic/chat/" + gid, gp);
        }
    }
}
