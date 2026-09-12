package com.example.World.Messages;

import com.example.World.Blocks.BlockService;
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
import java.util.Set;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final GroupService groupService;
    private final GroupUserRepository groupUserRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final BlockService blockService;

    public MessageService(MessageRepository messageRepository, GroupService groupService, GroupUserRepository groupUserRepository, UserRepository userRepository, NotificationService notificationService,
                          BlockService blockService) {
        this.messageRepository = messageRepository;
        this.groupService = groupService;
        // Injected but never assigned before, so the field did not exist at all.
        this.groupUserRepository = groupUserRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.blockService = blockService;
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
    public MessageView sendMessage(Long gid, Long senderId, String content, Integer mediaType ) {

        List<Groupuser_> memberships = groupUserRepository.findByGid(gid);

        if (memberships.stream().noneMatch(m -> m.uid().equals(senderId))) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "You are not a member of this conversation");
        }

        // Everyone in the conversation except whoever is sending.
        List<User_> recipients = memberships.stream()
                .map(Groupuser_::uid)
                .filter(uid -> !uid.equals(senderId))
                .distinct()
                .map(userRepository::findById)
                .flatMap(Optional::stream)
                .toList();

        // A block stops a one-to-one conversation, which is the case it exists
        // for. A larger group is left alone deliberately: silently dropping a
        // message to everybody because one member is blocked, or removing people
        // from a conversation they were invited to, are both worse than the
        // problem - and neither is what blocking was asked to mean here.
        if (recipients.size() == 1
                && blockService.blockedBetween(senderId, recipients.get(0).uid())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You cannot message this person");
        }

        Message_ message = new Message_(
           null,
                senderId,
                content,
                mediaType,
                new Date().getTime(),
                null,
                gid,
                null
        );

        Message_ msg = messageRepository.save(message);
        groupService.updateRecentData(gid,msg.mid());

        // Anyone with this conversation open has, by definition, read what just
        // arrived in it. Recording that now keeps last_read_timestamp the single
        // answer to "has this been read" - the alternative is every reader of that
        // timestamp also having to ask who happens to be looking at this moment.
        long readUpTo = markWatchersCaughtUp(gid, memberships, recipients, senderId, msg.created_at());

        String body = mediaType == 0 ? content : mediaType == 1 ? "Photo was sent" : "Video was sent";

        // Skip only the people who are already looking at this conversation. This
        // used to compare the sender's status to the recipient's, which happens to
        // be equal when both are watching the same chat - but also when both are
        // merely offline, so an offline recipient was silently denied the push that
        // was the whole point of the notification.
        List<User_> toNotify = recipients.stream()
                .filter(recipient -> !recipient.status().equals(watchingChat(gid)))
                .toList();

        if (!toNotify.isEmpty()) {
            // Handed over whole rather than one call per member: the fan-out is a
            // single multicast now instead of a blocking Firebase call each, all of
            // which ran before the message reached the topic.
            notificationService.notifyConversation(
                    userRepository.findById(senderId).orElseThrow(),
                    gid,
                    groupService.conversationNameOf(gid),
                    toNotify,
                    body);
        }
        // Return the saved row, not the pre-save object: `message` was built with
        // mid = null and only `msg` carries the generated id. MessageController
        // broadcasts this over /topic/chat/{gid}, so every live message reached
        // clients with mid = null - which collided as duplicate React keys and
        // left live messages unmatchable for delete and read-receipt calls.
        return MessageView.of(msg, readUpTo);
    }

    /**
     * Advances the read timestamp of every member who currently has this
     * conversation open, and returns the resulting read horizon.
     *
     * Without this a message sent to somebody sitting in the chat stayed unseen to
     * its sender until that person left and came back: their timestamp was from
     * when they opened the conversation, which is necessarily before anything sent
     * afterwards. The old per-message column got this right by consulting status
     * at send time, and dropping it lost the behaviour with it.
     */
    private long markWatchersCaughtUp(Long gid, List<Groupuser_> memberships,
                                      List<User_> recipients, Long senderId, long sentAt) {
        Set<Long> watching = recipients.stream()
                .filter(recipient -> recipient.status().equals(watchingChat(gid)))
                .map(User_::uid)
                .collect(Collectors.toSet());

        long earliest = Long.MAX_VALUE;
        boolean anybodyElse = false;

        for (Groupuser_ membership : memberships) {
            if (membership.uid().equals(senderId)) {
                continue;
            }
            anybodyElse = true;

            long readAt = membership.last_read_timestamp();

            if (watching.contains(membership.uid()) && readAt < sentAt) {
                groupUserRepository.advanceReadTimestamp(membership.guid(), sentAt);
                readAt = sentAt;
            }

            earliest = Math.min(earliest, readAt);
        }

        // Nobody else is in the conversation, so nobody has read it.
        return anybodyElse ? earliest : Long.MIN_VALUE;
    }

    /**
     * The moment before which every other member has read this conversation.
     *
     * A message counts as read once it was created at or before this - which is
     * the sender's seen-tick. Taking the earliest means "everyone", so one member
     * who has not looked yet holds the whole conversation unread, rather than the
     * old column's answer about whichever single recipient it happened to name.
     */
    private static long readUpTo(List<Groupuser_> memberships, Long viewerUid) {
        return memberships.stream()
                .filter(m -> !m.uid().equals(viewerUid))
                .mapToLong(Groupuser_::last_read_timestamp)
                .min()
                // Nobody else is in the conversation, so nobody has read it.
                .orElse(Long.MIN_VALUE);
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

    /** A single message, provided the caller belongs to the conversation it is in. */
    public MessageView getMessage(Long mid, Long uid) {
        Message_ message = messageRepository.findById(mid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "message not found"));

        requireMembership(message.gid(), uid);

        return MessageView.of(message, readUpTo(groupUserRepository.findByGid(message.gid()), uid));
    }

    /** Returns a conversation's messages, provided the caller is a member of it. */
    public List<MessageView> getChatMessages(Long gid, Long uid) {
        requireMembership(gid, uid);

        // One read horizon for the whole conversation rather than a stored flag
        // per message, so this stays a single extra query however long the history.
        long readUpTo = readUpTo(groupUserRepository.findByGid(gid), uid);

        return messageRepository.findMessagesByGidAsc(gid).stream()
                .map(message -> MessageView.of(message, readUpTo))
                .toList();
    }

    public void requireMembership(Long gid, Long uid) {
        if(!groupService.isMember(gid, uid)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this conversation");
        }
    }

    /**
     * Records that the caller has read this conversation up to now.
     *
     * This used to walk every unread message in the conversation and flip a column
     * on each one - a write per message to say one thing about one reader, and a
     * thing the message was the wrong place to keep. It is one row now: the
     * caller's own membership.
     */
    public void updatePrevReadReceipts(Long uid, Long gid){
        groupService.updateLastReadTimestamp(gid, uid);
    }

    public List<ConversationDTO> getConversations( Long uid ){
        // The caller's own membership row for each group, keyed by gid. This used to
        // be zipped against the group list by position, which only held while both
        // queries returned the same number of rows in the same order - a group row
        // missing for any reason shifted every later pairing or ran off the end.
        Map<Long, Groupuser_> membershipByGid = groupService.getGroupProfiles(uid).stream()
                .collect(Collectors.toMap(Groupuser_::gid, gu -> gu, (first, second) -> first));

        List<Group_> groups = groupService.getUserGroups(uid);

        // Every conversation's membership, in one query. Both the counterparty of a
        // direct conversation and the point up to which the others have read come
        // from these rows, and asking per conversation would make the cost of the
        // messages screen grow with how much the user uses the app.
        Map<Long, List<Groupuser_>> membersByGid =
                groupService.getMembershipsOf(groups.stream().map(Group_::gid).toList());

        Map<Long, Long> counterpartByGid = new java.util.HashMap<>();
        membersByGid.forEach((gid, members) -> {
            List<Long> others = members.stream()
                    .map(Groupuser_::uid)
                    .filter(other -> !other.equals(uid))
                    .toList();
            // Exactly one other person means there is somebody to name the
            // conversation after; more than one and it carries its own name.
            if (others.size() == 1) {
                counterpartByGid.put(gid, others.get(0));
            }
        });

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
                    !isDirect,
                    MessageView.of(lastMessage,
                            readUpTo(membersByGid.getOrDefault(group.gid(), List.of()), uid)),
                    // Unread for this reader: it arrived after they last opened the
                    // conversation, and they are not the one who sent it. The same
                    // rule for two people or twenty, where the old per-message flag
                    // could only ever describe one recipient.
                    lastMessage.created_at() > gUser.last_read_timestamp()
                            && !lastMessage.uid().equals(uid),
                    other == null ? null : other.profile_picture(),
                    group.gid()));
        }

        return convoList.stream()
                .sorted(Comparator.comparingLong((ConversationDTO c) -> c.lastMessage().created_at()).reversed())
                .toList();
    }


}
