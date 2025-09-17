package com.example.World.Messages;

import com.example.World.Groups.*;
import com.example.World.Notifications.NotificationDTO;
import com.example.World.Notifications.NotificationService;
import com.example.World.Notifications.Notification_;
import com.example.World.Users.UserRepository;
import com.example.World.Users.User_;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

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
        return message;
    }

    public List<Message_> getChatMessages(Long gid) {
        return messageRepository.findMessagesByGidAsc(gid);
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
        List<ConversationDTO> convoList = new ArrayList<>();

        List<Group_> groups =  groupService.getUserGroups(uid)
                .stream()
                .sorted(Comparator.comparingLong(Group_::gid))
                .toList();
        List<Groupuser_> gu =  groupService.getGroupProfiles(uid)
                .stream()
                .sorted(Comparator.comparingLong(Groupuser_::gid))
                .toList();

        for (int i = 0; i < gu.size(); i++) {
            Group_ group = groups.get(i);
            Groupuser_ gUser = gu.get(i);
            User_ other = userRepository.findById(gUser.other_uid()).orElseThrow();
            Message_ lastMessage = messageRepository.findById(group.last_mid()).orElseThrow();


            if(group.sort() == 0) {
                ConversationDTO temp = new ConversationDTO(other.user_name(), other.uid(),
                        lastMessage,lastMessage.is_read(),other.profile_picture(), group.gid()); // TODO replace unread (useless)

                convoList.add(temp);
            }else{
                ConversationDTO temp = new ConversationDTO(group.group_name(), other.uid(),
                        lastMessage,lastMessage.is_read(),other.profile_picture(),group.gid()); // TODO replace unread (useless)

                convoList.add(temp);
            }
        }


        return convoList.stream().sorted(Comparator.comparingLong(c -> c.lastMessage().created_at()))
                .toList()
                .reversed();
    }


}
