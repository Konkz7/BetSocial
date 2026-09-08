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

    /**
     * Opens the direct conversation with someone, creating it only if there is not
     * one already. It used to create a second conversation unconditionally, beside
     * whatever was already there, and pass a name of the two uids concatenated
     * that nothing ever displayed.
     */
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/make/{otherUid}")
    Group_ makeDMGroup(@PathVariable Long otherUid, HttpSession session) {
        return groupService.openDirectConversation(requireUserId(session), otherUid);
    }


    @PutMapping("/update-timestamp/{gid}")
    void updateTimestamp(@PathVariable Long gid, HttpSession session) {
        Long uid = (Long) session.getAttribute("userId");
        groupService.updateLastReadTimestamp(gid,uid);
    }

    // --- group lifecycle --------------------------------------------------
    //
    // GroupService.createGroup already existed but had no endpoint and no caller,
    // so a group could not be made at all. These expose it and the operations a
    // group needs to be usable.
    //
    // The permission model, applied in GroupService: any member may add someone,
    // only an administrator may remove or rename, anyone may leave, the creator is
    // the first administrator, and a group is soft-deleted once the last member
    // has left.

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/create")
    Group_ createGroup(@Valid @RequestBody GroupCreateDTO request, HttpSession session) {
        Long uid = requireUserId(session);
        return groupService.createGroup(request.group_name(), uid, request.members());
    }

    @GetMapping("/members/{gid}")
    List<GroupMemberView> getMembers(@PathVariable Long gid, HttpSession session) {
        return groupService.getMembers(gid, requireUserId(session));
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/add-member/{gid}/{newUid}")
    Groupuser_ addMember(@PathVariable Long gid, @PathVariable Long newUid, HttpSession session) {
        return groupService.addMember(gid, requireUserId(session), newUid);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/remove-member/{gid}/{targetUid}")
    void removeMember(@PathVariable Long gid, @PathVariable Long targetUid, HttpSession session) {
        groupService.removeMember(gid, requireUserId(session), targetUid);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/leave/{gid}")
    void leaveGroup(@PathVariable Long gid, HttpSession session) {
        groupService.leaveGroup(gid, requireUserId(session));
    }

    @PutMapping("/rename/{gid}")
    Group_ renameGroup(@PathVariable Long gid, @Valid @RequestBody GroupRenameDTO request,
                       HttpSession session) {
        return groupService.renameGroup(gid, requireUserId(session), request.group_name());
    }

    /**
     * Deletes a conversation and everything in it. Administrators only, and never
     * automatic - an abandoned group stays so that the people who were in it keep
     * their record of having been there.
     */
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/delete/{gid}")
    void deleteGroup(@PathVariable Long gid, HttpSession session) {
        groupService.deleteGroup(gid, requireUserId(session));
    }

    /** The conversations the caller used to be in, whether they left or were removed. */
    @GetMapping("/past-groups")
    List<Groupuser_> pastGroups(HttpSession session) {
        return groupService.getPastMemberships(requireUserId(session));
    }

    private static Long requireUserId(HttpSession session) {
        Long uid = (Long) session.getAttribute("userId");
        if (uid == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        return uid;
    }
}
