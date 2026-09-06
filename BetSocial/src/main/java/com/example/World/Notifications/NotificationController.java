package com.example.World.Notifications;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
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
    public int deleteNotifications(@PathVariable Long nid, HttpSession session){
        Long uid = (Long) session.getAttribute("userId");
        if(uid == null){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");
        }

        Notification_ notification = notificationRepository.findById(nid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        // Previously unchecked: any authenticated user could dismiss anyone's notifications.
        if(!notification.uid().equals(uid)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the owner of this notification");
        }

        return notificationRepository.remove(nid);
    }

    @PutMapping("/update-read-markers")
    public void readNotifications(HttpSession session){
        Long uid = (Long) session.getAttribute("userId");
        notificationService.readNotifications(uid);
    }


}
