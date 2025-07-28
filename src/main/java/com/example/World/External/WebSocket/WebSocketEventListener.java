package com.example.World.External.WebSocket;



import com.example.World.Messages.GroupStatus;
import com.example.World.Messages.Message_;
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

    public WebSocketEventListener(SimpMessageSendingOperations messageOperations) {
        this.messageOperations = messageOperations;
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event){
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Long id = (Long) headerAccessor.getSessionAttributes().get("userId");

        if(Objects.nonNull(id)){
           System.out.println("User disconnected: " + id);

           GroupStatus gp = new GroupStatus(id,false);
           messageOperations.convertAndSend("/topic/chat", gp);
        }
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        // Example: extract userId from headers or session
        // If you have custom auth, you'd typically pass userId via a header
        String userIdStr = headerAccessor.getFirstNativeHeader("userId");

        if (userIdStr != null) {
            Long id = Long.parseLong(userIdStr);

            // Store in session attributes if needed
            headerAccessor.getSessionAttributes().put("userId", id);

            System.out.println("User connected: " + id);

            GroupStatus gp = new GroupStatus(id, false);
            messageOperations.convertAndSend("/topic/chat", gp);
        }
    }
}
