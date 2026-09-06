package com.example.World.Messages;

import com.example.World.Groups.*;
import com.example.World.Notifications.NotificationDTO;
import com.example.World.Notifications.NotificationService;
import com.example.World.Notifications.Notification_;
import com.example.World.Users.UserRepository;
import com.example.World.Users.User_;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final GroupService groupService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public MessageService(MessageRepository messageRepository, GroupService groupService, GroupUserRepository groupUserRepository, UserRepository userRepository, NotificationService notificationService) {
        this.messageRepository = messageRepository;
        this.groupService = groupService;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    public Message_ sendMessage(Long gid, Long senderId, Long recipientId , String content, Integer mediaType ) {

        User_ recipient = userRepository.findById(recipientId).orElseThrow();
        User_ sender = userRepository.findById(senderId).orElseThrow();

        boolean is_read = recipient.status().equals("online/chat/" + gid);

        Message_ message = new Message_(
           null,
                senderId,
                recipientId,
                content,
                mediaType,
                new Date().getTime(),
                null,
                gid,
                is_read,
                null
        );




        Message_ msg = messageRepository.save(message);
        groupService.updateRecentData(gid,msg.mid());

        if(!(sender.status().equals(recipient.status()))) {
            NotificationDTO temp = new NotificationDTO(senderId, "message", gid, "user");


            notificationService.registerNotification(recipient.fb_notification_token(),
                    mediaType == 0 ? content : mediaType == 1 ? "Photo was sent" : "Video was sent", temp, recipientId);

        }
        // Return the saved row, not the pre-save object: `message` was built with
        // mid = null and only `msg` carries the generated id. MessageController
        // broadcasts this over /topic/chat/{gid}, so every live message reached
        // clients with mid = null - which collided as duplicate React keys and
        // left live messages unmatchable for delete and read-receipt calls.
        return msg;
    }

    /** Soft-deletes a message. Only its sender may do so. */
    public void deleteMessage(Long mid, Long uid){
        Message_ message = messageRepository.findById(mid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        if(!message.uid().equals(uid)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the sender of this message");
        }

        messageRepository.softDelete(mid, new Date().getTime());
    }

    /** Returns a conversation's messages, provided the caller is a member of it. */
    public List<Message_> getChatMessages(Long gid, Long uid) {
        requireMembership(gid, uid);
        return messageRepository.findMessagesByGidAsc(gid);
    }

    public void requireMembership(Long gid, Long uid) {
        if(!groupService.isMember(gid, uid)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this conversation");
        }
    }


    public void updatePrevReadReceipts(Long uid, Long gid){

        List<Message_> messages = messageRepository.findMessagesByGidAscAndRead(gid);

        for(Message_ m : messages){
            if(!m.uid().equals(uid)) {
                messageRepository.updateReadReceipt(m.mid());
            }
        }
    }

    public List<ConversationDTO> getConversations( Long uid ){
        // The caller's own membership row for each group, keyed by gid. This used to
        // be zipped against the group list by position, which only held while both
        // queries returned the same number of rows in the same order - a group row
        // missing for any reason shifted every later pairing or ran off the end.
        Map<Long, Groupuser_> membershipByGid = groupService.getGroupProfiles(uid).stream()
                .collect(Collectors.toMap(Groupuser_::gid, gu -> gu, (first, second) -> first));

        List<ConversationDTO> convoList = new ArrayList<>();

        for (Group_ group : groupService.getUserGroups(uid)) {
            Groupuser_ gUser = membershipByGid.get(group.gid());
            if (gUser == null) {
                continue;
            }

            // A conversation with no messages yet has last_mid null - exactly what
            // createDMGroup produces. Dereferencing it threw "Id must not be null"
            // and took down the caller's entire conversation list, not just this row.
            if (group.last_mid() == null) {
                continue;
            }

            Message_ lastMessage = messageRepository.findById(group.last_mid()).orElse(null);
            if (lastMessage == null) {
                continue;
            }

            // other_uid identifies the counterparty in a direct message. Group chats
            // have no single counterparty, so it is null there and only the group's
            // own name is available.
            User_ other = gUser.other_uid() == null
                    ? null
                    : userRepository.findById(gUser.other_uid()).orElse(null);

            boolean isDirectMessage = group.sort() == 0;
            if (isDirectMessage && other == null) {
                continue;
            }

            convoList.add(new ConversationDTO(
                    isDirectMessage ? other.user_name() : group.group_name(),
                    other == null ? null : other.uid(),
                    lastMessage,
                    lastMessage.is_read(),
                    other == null ? null : other.profile_picture(),
                    group.gid()));
        }

        return convoList.stream()
                .sorted(Comparator.comparingLong((ConversationDTO c) -> c.lastMessage().created_at()).reversed())
                .toList();
    }


}
