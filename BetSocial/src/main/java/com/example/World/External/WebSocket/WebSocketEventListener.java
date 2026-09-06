package com.example.World.External.WebSocket;



import com.example.World.Messages.GroupStatus;
import com.example.World.Messages.Message_;
import com.example.World.Groups.GroupService;
import com.example.World.Security.WebSocketPrincipal;
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
    private final GroupService groupService;

    public WebSocketEventListener(SimpMessageSendingOperations messageOperations, UserRepository userRepository,
                                  GroupService groupService) {
        this.messageOperations = messageOperations;
        this.userRepository = userRepository;
        this.groupService = groupService;
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

        // Identity comes from the principal Spring Security established during the
        // HTTP handshake. The "userId" native header the client still sends is
        // deliberately ignored - trusting it let any client impersonate any user.
        Long id = WebSocketPrincipal.userIdOf(headerAccessor.getUser());
        String chatIdStr = headerAccessor.getFirstNativeHeader("chatId");

        if (id == null || chatIdStr == null) {
            return;
        }

        Long gid = Long.parseLong(chatIdStr);

        // chatId is routing information, not identity, but it still decides which
        // topic this user's presence is broadcast to - so it must be a conversation
        // they actually belong to.
        if (!groupService.isMember(gid, id)) {
            System.out.println("Rejected presence for user " + id + " on chat " + gid + ": not a member");
            return;
        }

        headerAccessor.getSessionAttributes().put("userId", id);
        headerAccessor.getSessionAttributes().put("chatId", gid);

        System.out.println("User " + id + " connected to chat " + gid);
        userRepository.changeStatus(id,"online/chat/" + gid);

        GroupStatus gp = new GroupStatus(id, gid, true , true);
        messageOperations.convertAndSend("/topic/chat/" + gid, gp);
    }
}
