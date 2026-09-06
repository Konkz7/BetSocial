package com.example.World.Groups;


import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


import java.util.List;
import java.util.Optional;

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

    @GetMapping("/all")
    List<Group_> findAll(){
        return groupRepository.findAll();
    }

    @GetMapping("/{gid}")
    Group_ findById(@PathVariable Long gid){
        Optional<Group_> group = groupRepository.findById(gid);
        if(group.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "group not found");
        }
        return group.get();
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
/*
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/update/{gid}")
    void update(@Valid @RequestBody Group group, @PathVariable Integer gid){
        groupRepository.updateGroup(gid, group.result(), group.amount(), group.status());
    }

 */
    void delete(@PathVariable Long gid){
        groupRepository.delete(groupRepository.findById(gid).get());
    }
}
