package com.example.World.Notifications;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository){
        this.notificationRepository = notificationRepository;
    }

    @GetMapping("/active-notifications")
    public List<Notification_> getActiveNotifications(HttpSession session){
        Long uid = (Long) session.getAttribute("userId");
        return notificationRepository.getActiveNotifications(uid);
    }

    @PutMapping("/delete/{nid}")
    public int getActiveNotifications(@PathVariable Long nid){
        return notificationRepository.remove(nid);
    }
}
