package com.example.World.Groups;

import com.example.World.Users.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;


import java.util.*;
import java.util.stream.Collectors;

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

    /**
     * Creates a conversation.
     *
     * A null name makes a direct conversation, which must be between exactly two
     * people and is closed to anyone else. Any other conversation is a group and
     * needs a name - there would otherwise be nothing to call it.
     */
    @Transactional
    public Group_ createGroup(String name ,Long creatorId, List<Long> users) {
        String groupName = name == null ? null : requireValidName(name);

        // The creator is added as a member below, so naming them again in the
        // list would give them two membership rows in the same conversation.
        List<Long> members = (users == null ? List.<Long>of() : users).stream()
                .filter(Objects::nonNull)
                .filter(uid -> !uid.equals(creatorId))
                .distinct()
                .toList();

        if (groupName == null && members.size() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A conversation with more or fewer than two people is a group and needs a name");
        }

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
            null,
            time,
   null,
    null
        );


        group = groupRepository.save(group);

        // The creator is the first administrator - otherwise a new group would
        // have nobody able to rename it or remove anyone. It means nothing in a
        // direct conversation, where there is nothing to administer.
        groupUserRepository.save(membershipRow(group.gid(), creatorId, time, true));

        for(Long user: members){
            groupUserRepository.save(membershipRow(group.gid(), user, time, false));
        }


        return group;
    }

    /**
     * The direct conversation between two people, creating it if they have not
     * spoken before. Reusing the existing one is what stops a second conversation
     * appearing alongside the first every time someone opens a profile.
     */
    @Transactional
    public Group_ openDirectConversation(Long uid, Long otherUid) {
        if (uid.equals(otherUid)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You cannot start a conversation with yourself");
        }

        requireUserExists(otherUid);

        return groupRepository.findDirectConversation(uid, otherUid)
                .orElseGet(() -> createGroup(null, uid, List.of(otherUid)));
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

        // Soft-deleted, not removed: being removed from a group is something the
        // person needs to be able to see afterwards, and a vanished row cannot be
        // told apart from never having been a member.
        groupUserRepository.softDelete(target.guid(), new Date().getTime());
    }

    /**
     * Leaves a group. Anyone may leave, including the last administrator: the
     * role is passed on rather than the exit being blocked.
     */
    @Transactional
    public void leaveGroup(Long gid, Long uid) {
        Groupuser_ membership = requireMembership(gid, uid);
        requireGroupConversation(gid);

        groupUserRepository.softDelete(membership.guid(), new Date().getTime());

        List<Groupuser_> remaining = groupUserRepository.findByGid(gid);

        // A group with nobody left in it is deliberately *not* deleted here.
        // Deleting it would cascade away every membership row, including the
        // soft-deleted ones that are the only record anyone was ever in it - so
        // emptying a group would quietly destroy the history it is meant to keep.
        // Removing a conversation for good is an explicit act: see deleteGroup.
        if (remaining.isEmpty()) {
            return;
        }

        // If that was the last administrator the group would otherwise be stuck
        // forever - nobody able to rename it, remove anyone, or delete it. The
        // longest-standing remaining member takes the role.
        if (remaining.stream().noneMatch(Groupuser_::administrator)) {
            Groupuser_ successor = remaining.stream()
                    .min(Comparator.comparingLong(Groupuser_::created_at)
                            .thenComparing(Groupuser_::guid))
                    .orElseThrow();

            groupUserRepository.updateAdministrator(successor.guid(), true);
        }
    }

    /**
     * Deletes a conversation outright. Administrators only.
     *
     * This is the one destructive operation on a group and the only way one is
     * ever removed. The foreign keys cascade it to both the messages and every
     * membership row, so nothing is left pointing at a group that has gone.
     * Nothing does this automatically - an abandoned group simply stays, unseen,
     * so that the people who were in it keep their record of having been there.
     */
    @Transactional
    public void deleteGroup(Long gid, Long actorUid) {
        Groupuser_ actor = requireMembership(gid, actorUid);
        requireGroupConversation(gid);

        if (!actor.administrator()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only an administrator can delete this group");
        }

        groupRepository.deleteById(gid);
    }

    /** The conversations a user used to be in, most recently left first. */
    public List<Groupuser_> getPastMemberships(Long uid) {
        return groupUserRepository.findPastMembershipsByUid(uid);
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
        return new Groupuser_(null, gid, uid, time, time, administrator, null);
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
     * These operations only make sense for a named group.
     *
     * A direct conversation is closed: adding a third person to it would give a
     * stranger the whole of a private history that two people had every reason to
     * expect stayed between them. Starting a new group is the way to widen a
     * conversation, and it begins empty.
     *
     * Having no name is what makes a conversation direct - see Group_. There is no
     * separate kind, so there is no separate flag to consult.
     */
    private Group_ requireGroupConversation(Long gid) {
        // group_.deleted_at is deliberately not consulted: a group is only ever
        // hard-deleted now, so the row being absent is the whole signal. That
        // column and its V3 check constraint are left over and want dropping.
        Group_ group = groupRepository.findById(gid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        if (group.group_name() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This is a direct conversation, not a group");
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

    // createDMGroup is gone. It built a second kind of conversation by hand -
    // sort = 0, a name of the two uids concatenated that was shown to nobody, and
    // a pair of membership rows each pointing at the other person. All three said
    // the same thing the membership rows already said. openDirectConversation
    // creates the same thing through the one creation path.

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

    /**
     * The current membership of each of the given conversations, keyed by gid.
     *
     * One query for all of them rather than a lookup per conversation: this feeds
     * the conversation list, which is fetched on every visit to the messages
     * screen, and both who a conversation is with and how far the others have read
     * it come out of these same rows.
     */
    public Map<Long, List<Groupuser_>> getMembershipsOf(List<Long> gids) {
        if (gids.isEmpty()) {
            return Map.of();
        }

        return groupUserRepository.findByGidIn(gids).stream()
                .collect(Collectors.groupingBy(Groupuser_::gid));
    }

    /** True when the user belongs to the group - used to gate access to its messages. */
    public boolean isMember(Long gid, Long uid) {
        return groupUserRepository.findByGidandUid(gid, uid).isPresent();
    }


    /**
     * The gid of an existing direct conversation between two people, or null.
     *
     * This used to load every conversation each of them belonged to, intersect the
     * two sets, then fetch each shared conversation in turn to test its sort -
     * work proportional to how much they each use the app, to answer a question
     * about one row. The database answers it directly now.
     */
    public Long sameGroupCheck(Long uid1, Long uid2){
        return groupRepository.findDirectConversation(uid1, uid2)
                .map(Group_::gid)
                .orElse(null);
    }

}
