package com.example.World.Notifications;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    public NotificationController(NotificationRepository notificationRepository, NotificationService notificationService){
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
    }

    @GetMapping("/active-notifications")
    public List<Notification_> getActiveNotifications(HttpSession session){
        Long uid = (Long) session.getAttribute("userId");
        return notificationRepository.getActiveNotifications(uid);
    }

    @PutMapping("/delete/{nid}")
    public int deleteNotifications(@PathVariable Long nid){
        return notificationRepository.remove(nid);
    }

    @PutMapping("/update-read-markers")
    public void readNotifications(HttpSession session){
        Long uid = (Long) session.getAttribute("userId");
        notificationService.readNotifications(uid);
    }


}
