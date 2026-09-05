package com.example.World.Groups;

import org.springframework.stereotype.Service;


import java.util.*;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupUserRepository groupUserRepository;

    public GroupService(GroupRepository groupRepository, GroupUserRepository groupUserRepository) {
        this.groupRepository = groupRepository;
        this.groupUserRepository = groupUserRepository;
    }

    public Group_ createGroup(String name ,Long creatorId, List<Long> users) {
        Long time = new Date().getTime();
        Group_ group = new Group_(
            null,
            name,
            1,
            null,
            time,
   null,
    null
        );


        group = groupRepository.save(group);

        Groupuser_ groupUser = new Groupuser_(
                null,
                group.gid(),
                creatorId,
                null,
                time,
                time,
                true
        );

        groupUserRepository.save(groupUser);

        for(Long user: users){

            Groupuser_ GUser = new Groupuser_(
                    null,
                    group.gid(),
                    user,
                    null,
                    time,
                    time,
                    false
            );

            groupUserRepository.save(GUser);
        }


        return group;
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
