package com.example.World.Messages;

import com.example.World.Groups.*;
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

    public MessageService(MessageRepository messageRepository, GroupService groupService, GroupUserRepository groupUserRepository, UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.groupService = groupService;
        this.userRepository = userRepository;
    }

    public Message_ sendMessage(Long gid, Long senderId, Long recipientId , String content, Integer mediaType) {


        Message_ message = new Message_(
           null,
                senderId,
                recipientId,
                content,
                mediaType,
                new Date().getTime(),
                null,
                gid,
                false,
                null

        );


        messageRepository.save(message);
        if(message.media_type() == 1){
            groupService.updateRecentData(gid,"Photo was sent", message.created_at());
        }else if(message.media_type() == 2){
            groupService.updateRecentData(gid,"Video was sent", message.created_at());
        }else{
            groupService.updateRecentData(gid,content, message.created_at());
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
        List<Groupuser_> gu =  groupService.getGroupProfiles(uid)
                .stream()
                .sorted(Comparator.comparingLong(Groupuser_::gid)) 
                .toList();
        List<Group_> groups =  groupService.getUserGroups(uid)
                .stream()
                .sorted(Comparator.comparingLong(Group_::gid)) 
                .toList();

        for (int i = 0; i < gu.size(); i++) {
            Group_ group = groups.get(i);
            Groupuser_ gUser = gu.get(i);
            User_ other = userRepository.findById(gUser.other_uid()).orElseThrow();
            boolean unread = true;

            if(group.last_time() < gUser.last_read_timestamp()){
                unread = false;
            }

            if(group.sort() == 0) {
                ConversationDTO temp = new ConversationDTO(other.user_name(), other.uid(),
                        group.last_message(),group.last_time(),unread,other.profile_picture(), group.gid()); // TODO replace with avatar

                convoList.add(temp);
            }else{
                ConversationDTO temp = new ConversationDTO(group.group_name(), other.uid(),
                        group.last_message(),group.last_time(),unread,other.profile_picture(),group.gid()); // TODO replace with avatar

                convoList.add(temp);
            }
        }



        return convoList.stream().sorted(Comparator.comparingLong(ConversationDTO::time)).toList().reversed();
    }


}
