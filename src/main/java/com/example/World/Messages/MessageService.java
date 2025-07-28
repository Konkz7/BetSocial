package com.example.World.Messages;

import com.example.World.Groups.*;
import org.springframework.stereotype.Service;


import java.util.Date;
import java.util.List;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final GroupService groupService;
    private final GroupUserRepository groupUserRepository;

    public MessageService(MessageRepository messageRepository, GroupService groupService, GroupUserRepository groupUserRepository) {
        this.messageRepository = messageRepository;
        this.groupService = groupService;
        this.groupUserRepository = groupUserRepository;
    }

    public Message_ sendMessage(Long gid, Long senderId, Long recipientId , String content) {

        //if message isnt sent to a group and the message is sent to a new person, create new group users.
        /*
        if(recipientId != null) {
            if (groupUserRepository.findByGidandUid(gid, senderId).isEmpty()) {
                Group_ tempGroup = groupService.createGroup(senderId + ""+ recipientId, 0, senderId);
                gid = tempGroup.gid();
                groupUserRepository.save(new Groupuser_(null, gid, recipientId, false));
            }
        }

         */

        Message_ message = new Message_(
           null,
                senderId,
                recipientId,
                content,
                new Date().getTime(),
                null,
                gid,
                false,
                null

        );


        messageRepository.save(message);
        groupService.updateRecentData(gid,content, message.created_at());



        return message;
    }

    public List<Message_> getChatMessages(Long gid) {
        return messageRepository.findMessagesByGidDesc(gid);
    }
}
