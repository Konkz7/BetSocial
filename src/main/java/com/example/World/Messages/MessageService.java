package com.example.World.Messages;

import com.example.World.Groups.GroupRepository;
import com.example.World.Groups.Group_;
import com.example.World.Users.UserRepository;
import com.example.World.Users.User_;
import org.springframework.stereotype.Service;


import java.util.Date;
import java.util.List;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public MessageService(MessageRepository messageRepository, GroupRepository chatRoomRepository, UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.groupRepository = chatRoomRepository;
        this.userRepository = userRepository;
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

        return messageRepository.save(message);
    }

    public List<Message_> getChatMessages(Long gid) {
        return messageRepository.findMessagesByGidDesc(gid);
    }
}
