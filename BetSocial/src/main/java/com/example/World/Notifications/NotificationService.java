package com.example.World.Notifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.World.Users.UserRepository;
import com.example.World.Users.User_;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    private void sendFBNotification(String token, Long uid, String title, String body, String type, Long target_id) throws Exception {
        Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putData("type", type)
                .putData("target_id", String.valueOf(target_id))
                .putData("uid", String.valueOf(uid))
                .build();

        String response = FirebaseMessaging.getInstance().send(message);
        log.debug("Push delivered to Firebase, id {}", response);
    }

    /**
     * Notifies everyone a message went to, in one pass.
     *
     * Sending to a conversation used to call registerNotification once per member,
     * and each of those re-read the sender, re-read the recipient the caller was
     * already holding, and made its own blocking call to Firebase. Since all of it
     * happens before the message is broadcast to the topic, a group of fifty made
     * everybody's live delivery wait on fifty sequential round trips to Google.
     *
     * The push is one multicast now. What still has to be per recipient is the
     * notification row - each person needs their own, and their own de-duplication
     * against it - but that is database work, not the network.
     *
     * The wording is what makes batching possible as well as what makes it read
     * correctly: every recipient of one message now gets the same text, so there is
     * a single payload to send.
     *
     *   direct   title: who sent it        body: what they said
     *   group    title: the group's name   body: who said it, then what
     *
     * It used to open with the *recipient's* own name - "emily: You have received a
     * message from chris" - which told you who you are and buried who it was from.
     */
    public void notifyConversation(User_ sender, Long gid, String conversationName,
                                   List<User_> recipients, String content) {
        if (recipients.isEmpty()) {
            return;
        }

        boolean isDirect = conversationName == null;
        String title = isDirect ? sender.user_name() : conversationName;
        String body = isDirect ? content : sender.user_name() + ": " + content;

        List<String> tokens = new java.util.ArrayList<>();

        for (User_ recipient : recipients) {
            // One row each, collapsed onto the previous one from the same sender in
            // the same conversation - so twenty messages leave one notification
            // rather than twenty.
            Optional<Notification_> existing = notificationRepository.findLatestNonDeleted(
                    "message", recipient.uid(), sender.uid(), gid);

            if (existing.isPresent()) {
                notificationRepository.changeContent(existing.get().nid(), body, new Date().getTime());
            } else {
                notificationRepository.save(new Notification_(null, recipient.uid(), sender.uid(),
                        "message", gid, "user", title, body, false, false, new Date().getTime()));
            }

            // A user who has never registered a device has no token. Passing null
            // through used to raise an exception per recipient, caught and logged.
            if (recipient.fb_notification_token() != null && !recipient.fb_notification_token().isBlank()) {
                tokens.add(recipient.fb_notification_token());
            }
        }

        if (tokens.isEmpty()) {
            return;
        }

        try {
            // Firebase caps a multicast at 500 tokens; a conversation caps at
            // GroupService.MAX_GROUP_MEMBERS, so one call always covers it.
            FirebaseMessaging.getInstance().sendEachForMulticast(MulticastMessage.builder()
                    .addAllTokens(tokens)
                    .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                    .putData("type", "message")
                    .putData("target_id", String.valueOf(gid))
                    .build());
        } catch (Exception e) {
            // A push that does not arrive must not fail the message that was sent.
            // The exception goes in as the last argument rather than concatenated,
            // so the stack trace is kept instead of e.toString().
            log.warn("Conversation push failed for group {}", gid, e);
        }
    }

    public void registerNotification(String token, String body , NotificationDTO notificationDTO , Long recipient_id ){

        String title;
        User_ sender = userRepository.findById(notificationDTO.actor_id()).orElseThrow();
        User_ recipient = userRepository.findById(recipient_id).orElseThrow();


        switch(notificationDTO.notification_type()){
            case "message":
                title = recipient.user_name() + ": You have received a message from " + sender.user_name();
                break;
            case "new_thread":
                title = recipient.user_name() + ": " + sender.user_name() + " posted a new thread!";
                break;
            case "new_comment":
                title = recipient.user_name() + ": " + sender.user_name() + " commented on your thread!";
                break;
            case "follow_request":
                title = recipient.user_name() + ": " + sender.user_name() + " sent you a follow!";
                break;
            case "thread_like":
                title = recipient.user_name() + ": " + sender.user_name() + " liked your thread!!";
                break;
            case "comment_like":
                title = recipient.user_name() + ": " + sender.user_name() + " liked your comment!";
                break;
            default:
                title = "Unknown";
        }

        Optional<Notification_> noti = notificationRepository.findLatestNonDeleted(notificationDTO.notification_type(),recipient_id,
                notificationDTO.actor_id(), notificationDTO.target_id());

        if(noti.isPresent()){
            notificationRepository.changeContent(noti.get().nid(),body, new Date().getTime());
        }else {
            notificationRepository.save(new Notification_(null, recipient_id, notificationDTO.actor_id(), notificationDTO.notification_type(),
                    notificationDTO.target_id(), notificationDTO.target_type(), title,body, false, false, new Date().getTime()));
        }

        try {
            sendFBNotification(token, recipient_id ,title, body, notificationDTO.notification_type(), notificationDTO.target_id());

        } catch (Exception e) {
            // The push token used to be printed here. A token is a credential for
            // sending to somebody's device, so it belongs in a log about as much
            // as a password does. The user it was for is enough to investigate.
            log.warn("Push to user {} failed", recipient_id, e);
        }


    }

    public void readNotifications(Long uid){
        List<Notification_> notis = notificationRepository.getActiveNotifications(uid);

        for(Notification_ n : notis){
            notificationRepository.updateReadMarkers(n.nid());
        }
    }

}
