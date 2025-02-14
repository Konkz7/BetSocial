package com.example.World.Groups;

import org.springframework.stereotype.Service;


import java.util.Date;
import java.util.List;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupUserRepository groupUserRepository;

    public GroupService(GroupRepository groupRepository, GroupUserRepository groupUserRepository) {
        this.groupRepository = groupRepository;
        this.groupUserRepository = groupUserRepository;
    }

    public Group_ createGroup(String name,Integer sort ,Long creatorId) {
        Group_ group = new Group_(
            null,
            name,
            sort,
            new Date().getTime(),
   null,
    null
        );

        GroupUser_ groupUser = new GroupUser_(
                null,
                group.gid(),
                creatorId,
                true
        );

        groupUserRepository.save(groupUser);

        return groupRepository.save(group);
    }

    public List<Group_> getUserGroups(Long uid) {
        List<GroupUser_> groupUsers = groupUserRepository.findByUid(uid);
        return groupRepository.findAllById(groupUsers.stream().map(GroupUser_::gid).toList());
    }
}
