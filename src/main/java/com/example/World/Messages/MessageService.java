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
}
