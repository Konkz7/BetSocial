package com.example.World.Notifications;
import com.example.World.Users.UserRepository;
import com.example.World.Users.User_;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    private void sendFBNotification(String token, String title, String body) throws Exception {
        Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putData("extraData", "some_value")
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
            default:
                title = "Unknown";
        }


        try {

            sendFBNotification(token, title, body);
            Optional<Notification_> noti = notificationRepository.findLatestUnread(notificationDTO.notification_type(),
                    notificationDTO.actor_id(), notificationDTO.target_id());

            if(noti.isPresent()){
                notificationRepository.changeContent(noti.get().nid(),body);
            }else {
                notificationRepository.save(new Notification_(null, recipient_id, notificationDTO.actor_id(), notificationDTO.notification_type(),
                        notificationDTO.target_id(), notificationDTO.target_type(), title,body, false, false, new Date().getTime()));
            }

        } catch (Exception e) {
            System.out.println("Notification Failed!");
            System.out.println(e);

        }
    }

}
