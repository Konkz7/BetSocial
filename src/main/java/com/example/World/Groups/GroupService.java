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
        Group_ group = new Group_(
            null,
            name,
            1,
            null,
            null,
            new Date().getTime(),
   null,
    null
        );


        group = groupRepository.save(group);

        Groupuser_ groupUser = new Groupuser_(
                null,
                group.gid(),
                creatorId,
                true
        );

        groupUserRepository.save(groupUser);

        for(Long user: users){

            Groupuser_ GUser = new Groupuser_(
                    null,
                    group.gid(),
                    user,
                    false
            );

            groupUserRepository.save(GUser);
        }


        return group;
    }

    public Group_ createDMGroup(String name ,Long uid , Long other_uid) {
        Group_ group = new Group_(
                null,
                name,
                0,
                null,
                null,
                new Date().getTime(),
                null,
                null
        );


        group = groupRepository.save(group);

        Groupuser_ user = new Groupuser_(
                null,
                group.gid(),
                uid,
                false
        );

        Groupuser_ other = new Groupuser_(
                null,
                group.gid(),
                other_uid,
                false
        );

        groupUserRepository.save(user);
        groupUserRepository.save(other);


        return group;
    }

    public int updateRecentData(Long gid, String message, Long message_time){
        return groupRepository.updateGroupRecentData(gid, message, message_time);
    }

    public List<Group_> getUserGroups(Long uid) {
        List<Groupuser_> groupUsers = groupUserRepository.findByUid(uid);
        return groupRepository.findAllById(groupUsers.stream().map(Groupuser_::gid).toList());
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
