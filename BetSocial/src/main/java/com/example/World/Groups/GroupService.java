package com.example.World.Groups;

import com.example.World.Users.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;


import java.util.*;

@Service
public class GroupService {

    /**
     * Chosen rather than derived: fan-out cost per message scales with this, and
     * a cap that exists from the start is far cheaper than one retrofitted onto
     * conversations that already exceed it.
     */
    public static final int MAX_GROUP_MEMBERS = 50;

    private static final int MAX_GROUP_NAME_LENGTH = 100;

    private final GroupRepository groupRepository;
    private final GroupUserRepository groupUserRepository;
    private final UserRepository userRepository;

    public GroupService(GroupRepository groupRepository, GroupUserRepository groupUserRepository,
                        UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.groupUserRepository = groupUserRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Group_ createGroup(String name ,Long creatorId, List<Long> users) {
        String groupName = requireValidName(name);

        // The creator is added as a member below, so naming them again in the
        // list would give them two membership rows in the same conversation.
        List<Long> members = (users == null ? List.<Long>of() : users).stream()
                .filter(Objects::nonNull)
                .filter(uid -> !uid.equals(creatorId))
                .distinct()
                .toList();

        if (members.size() + 1 > MAX_GROUP_MEMBERS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A group cannot have more than " + MAX_GROUP_MEMBERS + " members");
        }

        // groupuser_.uid has no foreign key to user_, so an unknown id would be
        // inserted silently and only surface as a member who cannot be rendered.
        for (Long uid : members) {
            requireUserExists(uid);
        }

        Long time = new Date().getTime();
        Group_ group = new Group_(
            null,
            groupName,
            1,
            null,
            time,
   null,
    null
        );


        group = groupRepository.save(group);

        // The creator is the first administrator - otherwise a new group would
        // have nobody able to rename it or remove anyone.
        groupUserRepository.save(membershipRow(group.gid(), creatorId, time, true));

        for(Long user: members){
            groupUserRepository.save(membershipRow(group.gid(), user, time, false));
        }


        return group;
    }

    /**
     * Adds a member. Any member may do this; removal is the restricted direction.
     */
    @Transactional
    public Groupuser_ addMember(Long gid, Long actorUid, Long newUid) {
        requireMembership(gid, actorUid);
        requireGroupConversation(gid);

        if (groupUserRepository.findByGidandUid(gid, newUid).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "That user is already a member of this group");
        }

        requireUserExists(newUid);

        if (groupUserRepository.findByGid(gid).size() >= MAX_GROUP_MEMBERS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This group already has the maximum of " + MAX_GROUP_MEMBERS + " members");
        }

        Long time = new Date().getTime();
        return groupUserRepository.save(membershipRow(gid, newUid, time, false));
    }

    /** Removes another member. Administrators only. */
    @Transactional
    public void removeMember(Long gid, Long actorUid, Long targetUid) {
        Groupuser_ actor = requireMembership(gid, actorUid);
        requireGroupConversation(gid);

        if (!actor.administrator()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only an administrator can remove a member");
        }

        if (actorUid.equals(targetUid)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Use leave to remove yourself from a group");
        }

        Groupuser_ target = groupUserRepository.findByGidandUid(gid, targetUid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "That user is not a member of this group"));

        // The actor is still a member, so this can never empty the group - only
        // leaving can, which is where the group is closed.
        groupUserRepository.delete(target);
    }

    /**
     * Leaves a group. Anyone may leave, including the last administrator: the
     * role is passed on rather than the exit being blocked.
     */
    @Transactional
    public void leaveGroup(Long gid, Long uid) {
        Groupuser_ membership = requireMembership(gid, uid);
        requireGroupConversation(gid);

        groupUserRepository.delete(membership);

        List<Groupuser_> remaining = groupUserRepository.findByGid(gid);

        if (remaining.isEmpty()) {
            // Nobody is left to read it, so the conversation is closed. The
            // messages stay; only the group row is marked.
            groupRepository.softDelete(gid, new Date().getTime());
            return;
        }

        // If that was the last administrator the group would otherwise be stuck
        // forever - nobody able to rename it, remove anyone, or close it. The
        // longest-standing remaining member takes the role.
        if (remaining.stream().noneMatch(Groupuser_::administrator)) {
            Groupuser_ successor = remaining.stream()
                    .min(Comparator.comparingLong(Groupuser_::created_at)
                            .thenComparing(Groupuser_::guid))
                    .orElseThrow();

            groupUserRepository.updateAdministrator(successor.guid(), true);
        }
    }

    /** Renames a group. Administrators only. */
    @Transactional
    public Group_ renameGroup(Long gid, Long actorUid, String name) {
        Groupuser_ actor = requireMembership(gid, actorUid);
        requireGroupConversation(gid);

        if (!actor.administrator()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only an administrator can rename this group");
        }

        groupRepository.updateName(gid, requireValidName(name));

        return groupRepository.findById(gid).orElseThrow();
    }

    /** The membership rows of a conversation the caller belongs to. */
    public List<Groupuser_> getMembers(Long gid, Long callerUid) {
        requireMembership(gid, callerUid);
        return groupUserRepository.findByGid(gid);
    }

    private Groupuser_ membershipRow(Long gid, Long uid, Long time, boolean administrator) {
        return new Groupuser_(null, gid, uid, null, time, time, administrator);
    }

    /**
     * Membership is checked before the group is looked up, so a caller who does
     * not belong is told the same thing whether or not the group exists.
     */
    private Groupuser_ requireMembership(Long gid, Long uid) {
        return groupUserRepository.findByGidandUid(gid, uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You are not a member of this group"));
    }

    /**
     * These operations only make sense for a real group. A direct message has a
     * fixed pair of participants; adding a third or leaving half of it would
     * leave a conversation the rest of the code cannot describe.
     */
    private Group_ requireGroupConversation(Long gid) {
        Group_ group = groupRepository.findById(gid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        if (group.deleted_at() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found");
        }

        if (group.sort() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This is a direct message, not a group");
        }

        return group;
    }

    private void requireUserExists(Long uid) {
        if (uid == null || userRepository.findById(uid).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + uid);
        }
    }

    private String requireValidName(String name) {
        String trimmed = name == null ? "" : name.trim();

        // group_.group_name carries a NOT NULL / non-empty check constraint and is
        // varchar(100); rejecting here gives a 400 instead of a 500 from the driver.
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A group needs a name");
        }
        if (trimmed.length() > MAX_GROUP_NAME_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A group name cannot be longer than " + MAX_GROUP_NAME_LENGTH + " characters");
        }

        return trimmed;
    }

    public Group_ createDMGroup(String name ,Long uid , Long other_uid) {
        Long time = new Date().getTime();

        Group_ group = new Group_(
                null,
                name,
                0,
                null,
                time,
                null,
                null
        );


        group = groupRepository.save(group);

        Groupuser_ user = new Groupuser_(
                null,
                group.gid(),
                uid,
                other_uid,
                time,
                time,
                false
        );

        Groupuser_ other = new Groupuser_(
                null,
                group.gid(),
                other_uid,
                uid,
                time,
                time,
                false
        );

        groupUserRepository.save(user);
        groupUserRepository.save(other);


        return group;
    }

    public int updateRecentData(Long gid, Long message){
        return groupRepository.updateGroupRecentData(gid, message);
    }

    public int updateLastReadTimestamp(Long gid , Long uid){
        Groupuser_ gu = groupUserRepository.findByGidandUid(gid,uid).orElseThrow();
        return groupUserRepository.updateReadTimestamp(gu.guid(), new Date().getTime());
    }

    public List<Group_> getUserGroups(Long uid) {
        List<Groupuser_> groupUsers = groupUserRepository.findByUid(uid);
        return groupRepository.findAllById(groupUsers.stream().map(Groupuser_::gid).toList());
    }

    public List<Groupuser_> getGroupProfiles(Long uid) {
        return groupUserRepository.findByUid(uid);
    }

    /** True when the user belongs to the group - used to gate access to its messages. */
    public boolean isMember(Long gid, Long uid) {
        return groupUserRepository.findByGidandUid(gid, uid).isPresent();
    }


    public Long sameGroupCheck(Long uid1, Long uid2){
        Set<Long> gid1 = new HashSet<>(groupUserRepository.findByUid(uid1).stream().map(Groupuser_::gid).toList());
        Set<Long> gid2 = new HashSet<>(groupUserRepository.findByUid(uid2).stream().map(Groupuser_::gid).toList());

        gid1.retainAll(gid2);

        for(Long l : gid1){
            if(groupRepository.findById(l).orElseThrow().sort() == 0){
                return l;
            }
        }

        return null;
    }

}
