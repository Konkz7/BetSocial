package com.example.World.Messages;

import com.example.World.Groups.GroupRepository;
import com.example.World.Groups.Group_;
import com.example.World.Users.UserRepository;
import com.example.World.Users.User_;

import java.time.LocalDateTime;
import java.util.List;

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
                LocalDateTime.now(),
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
