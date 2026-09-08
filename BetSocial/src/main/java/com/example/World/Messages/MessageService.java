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
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final GroupService groupService;
    private final GroupUserRepository groupUserRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public MessageService(MessageRepository messageRepository, GroupService groupService, GroupUserRepository groupUserRepository, UserRepository userRepository, NotificationService notificationService) {
        this.messageRepository = messageRepository;
        this.groupService = groupService;
        // Injected but never assigned before, so the field did not exist at all.
        this.groupUserRepository = groupUserRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /**
     * Sends a message to a conversation of any size.
     *
     * Who receives it is derived from the conversation's membership rows, never
     * supplied by the caller. The recipient used to be taken straight from the
     * client's STOMP payload, and it reaches registerNotification, which sends a
     * Firebase push whose body is the message text - so naming any uid there
     * delivered arbitrary text as a push to a user outside the conversation.
     *
     * A direct message is simply the case where that membership list has one
     * other person in it, so there is no separate DM path.
     */
    public Message_ sendMessage(Long gid, Long senderId, String content, Integer mediaType ) {

        if (groupUserRepository.findByGidandUid(gid, senderId).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "You are not a member of this conversation");
        }

        // Everyone in the conversation except whoever is sending.
        List<User_> recipients = groupUserRepository.findByGid(gid).stream()
                .map(Groupuser_::uid)
                .filter(uid -> !uid.equals(senderId))
                .distinct()
                .map(userRepository::findById)
                .flatMap(Optional::stream)
                .toList();

        // A single boolean cannot express "read by 3 of 5", so it only carries
        // meaning when there is exactly one other person - are they already
        // looking at this conversation. Replacing it with
        // groupuser_.last_read_timestamp is the next piece of work; until then a
        // message to a larger conversation is simply never pre-marked as read.
        boolean is_read = recipients.size() == 1
                && recipients.get(0).status().equals(watchingChat(gid));

        Message_ message = new Message_(
           null,
                senderId,
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

        String body = mediaType == 0 ? content : mediaType == 1 ? "Photo was sent" : "Video was sent";

        // target_id is the conversation, so registerNotification's existing
        // de-duplication collapses a burst of messages into one notification per
        // recipient rather than one per message.
        NotificationDTO notification = new NotificationDTO(senderId, "message", gid, "user");

        for (User_ recipient : recipients) {
            // Skip only the people who are already looking at this conversation.
            // This used to compare the sender's status to the recipient's, which
            // happens to be equal when both are watching the same chat - but also
            // when both are merely offline, so an offline recipient was silently
            // denied the push that was the whole point of the notification.
            if (recipient.status().equals(watchingChat(gid))) {
                continue;
            }

            notificationService.registerNotification(recipient.fb_notification_token(),
                    body, notification, recipient.uid());
        }
        // Return the saved row, not the pre-save object: `message` was built with
        // mid = null and only `msg` carries the generated id. MessageController
        // broadcasts this over /topic/chat/{gid}, so every live message reached
        // clients with mid = null - which collided as duplicate React keys and
        // left live messages unmatchable for delete and read-receipt calls.
        return msg;
    }

    /** The status a user carries while they have this conversation open. */
    private static String watchingChat(Long gid) {
        return "online/chat/" + gid;
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

        List<Group_> groups = groupService.getUserGroups(uid);

        // The counterparty of a direct conversation used to be read off
        // groupuser_.other_uid. It now comes from the membership rows, which is the
        // only place it was ever really recorded - fetched for every conversation
        // in one query rather than one lookup per row.
        Map<Long, Long> counterpartByGid = groupService.getCounterpartsOf(
                uid, groups.stream().map(Group_::gid).toList());

        Map<Long, User_> counterparts = userRepository.findAllById(
                        counterpartByGid.values().stream().distinct().toList())
                .stream()
                .collect(Collectors.toMap(User_::uid, u -> u, (first, second) -> first));

        List<ConversationDTO> convoList = new ArrayList<>();

        for (Group_ group : groups) {
            Groupuser_ gUser = membershipByGid.get(group.gid());
            if (gUser == null) {
                continue;
            }

            // A conversation with no messages yet has last_mid null - exactly what
            // opening a direct conversation produces. Dereferencing it threw "Id
            // must not be null" and took down the caller's entire conversation
            // list, not just this row.
            if (group.last_mid() == null) {
                continue;
            }

            Message_ lastMessage = messageRepository.findById(group.last_mid()).orElse(null);
            if (lastMessage == null) {
                continue;
            }

            // An unnamed conversation is a direct one and is titled by whoever else
            // is in it. A named one is a group and carries its own name.
            boolean isDirect = group.group_name() == null;
            Long counterpartId = counterpartByGid.get(group.gid());
            User_ other = counterpartId == null ? null : counterparts.get(counterpartId);

            if (isDirect && other == null) {
                continue;
            }

            convoList.add(new ConversationDTO(
                    isDirect ? other.user_name() : group.group_name(),
                    other == null ? null : other.uid(),
                    lastMessage,
                    // unread, not is_read. This was handed lastMessage.is_read(),
                    // the exact inverse of what the field means - harmless only
                    // because no client had started reading it yet.
                    !lastMessage.is_read() && !lastMessage.uid().equals(uid),
                    other == null ? null : other.profile_picture(),
                    group.gid()));
        }

        return convoList.stream()
                .sorted(Comparator.comparingLong((ConversationDTO c) -> c.lastMessage().created_at()).reversed())
                .toList();
    }


}
