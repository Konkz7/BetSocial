package com.example.World.Groups;


import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


import java.util.List;

@RequestMapping("/api/groups")
@RestController
public class GroupController {
    private final GroupRepository groupRepository;
    private final GroupService groupService;
    private final GroupUserRepository groupUserRepository;

    public GroupController(GroupRepository groupRepository, GroupService groupService, GroupUserRepository groupUserRepository) {
        this.groupRepository = groupRepository;
        this.groupService = groupService;
        this.groupUserRepository = groupUserRepository;
    }

    // GET /all is deliberately absent: it dumped every group in the database to
    // any authenticated caller, and there is no per-user scoping that would make
    // it meaningful. /user-groups already returns the caller's own conversations.

    @GetMapping("/{gid}")
    Group_ findById(@PathVariable Long gid, HttpSession session){
        Long uid = (Long) session.getAttribute("userId");

        // Membership is checked before the lookup so that a non-member cannot
        // tell an existing group from a missing one.
        if(!groupService.isMember(gid, uid)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this group");
        }

        return groupRepository.findById(gid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "group not found"));
    }

    // Returns an empty list rather than 404 when the user has no conversations.
    // "No results" is not an error, and the client surfaced the 404 as a
    // "Groups couldnt be found." alert to every user who had not started a chat.
    @GetMapping("/group-users")
    List<Groupuser_> findAllGroupUsers(HttpSession session){
        Long uid = (Long) session.getAttribute("userId");
        return groupUserRepository.findByUid(uid);
    }

    @GetMapping("/user-groups")
    List<Group_> findAllUsersGroups(HttpSession session){
        Long uid = (Long) session.getAttribute("userId");
        return groupService.getUserGroups(uid);
    }

    @GetMapping("/dm-check/{otherUid}")
    Long DMCheck(@PathVariable Long otherUid,HttpSession session){
        Long uid = (Long) session.getAttribute("userId");

        Long check = groupService.sameGroupCheck(uid,otherUid);

        /*
        if(check == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No groups found");
        }

         */

        return check;
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/make/{otherUid}")
    Group_ makeDMGroup(@PathVariable Long otherUid, HttpSession session) {
        Long uid = (Long) session.getAttribute("userId");
        return groupService.createDMGroup(uid + "" + otherUid,uid,otherUid);
    }


    @PutMapping("/update-timestamp/{gid}")
    void updateTimestamp(@PathVariable Long gid, HttpSession session) {
        Long uid = (Long) session.getAttribute("userId");
        groupService.updateLastReadTimestamp(gid,uid);
    }
}
