package com.example.World.Notifications;
import com.example.World.Users.UserRepository;
import com.example.World.Users.User_;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

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
        System.out.println("Sent message: " + response);
    }

    public void registerNotification(String token, String body , NotificationDTO notificationDTO , Long recipient_id ){

        String title;
        User_ sender = userRepository.findById(notificationDTO.actor_id()).orElseThrow();

        switch(notificationDTO.notification_type()){
            case "message":
                title = "You have received a message from: " + sender.user_name();
                break;
            case "new_thread":
                title = sender.user_name() + " posted a new thread!";
                break;
            case "new_comment":
                title = sender.user_name() + " commented on your thread!";
                break;
            case "follow_request":
                title = sender.user_name() + " sent you a follow!";
                break;
            case "thread_like":
                title = sender.user_name() + " liked your thread!!";
                break;
            case "comment_like":
                title = sender.user_name() + " liked your comment!";
                break;
            default:
                title = "Unknown";
        }

        Optional<Notification_> noti = notificationRepository.findLatestNonDeleted(notificationDTO.notification_type(),
                notificationDTO.actor_id(), notificationDTO.target_id());

        if(noti.isPresent()){
            notificationRepository.changeContent(noti.get().nid(),body, new Date().getTime());
        }else {
            notificationRepository.save(new Notification_(null, recipient_id, notificationDTO.actor_id(), notificationDTO.notification_type(),
                    notificationDTO.target_id(), notificationDTO.target_type(), title,body, false, false, new Date().getTime()));
        }

        try {
            System.out.println("TOKEN: " + token);
            sendFBNotification(token, recipient_id ,title, body, notificationDTO.notification_type(), notificationDTO.target_id());

        } catch (Exception e) {
            System.out.println("Notification Failed!");
            System.out.println(e);

        }


    }

    public void readNotifications(Long uid){
        List<Notification_> notis = notificationRepository.getActiveNotifications(uid);

        for(Notification_ n : notis){
            notificationRepository.updateReadMarkers(n.nid());
        }
    }

}
