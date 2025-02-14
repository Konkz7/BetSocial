package com.example.World.Groups;


import com.example.World.Bets.BetDTO;
import com.example.World.Bets.Bet_;
import com.example.World.Groups.Group_;
import com.example.World.Groups.GroupRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


import java.util.Date;
import java.util.List;
import java.util.Optional;

@RequestMapping("/api/groups")
@RestController
public class GroupController {
    private final GroupRepository groupRepository;

    public GroupController(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
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

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/create")
    void create(@Valid @RequestBody Group_ group){
        groupRepository.save(group);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/make")
    void makeBet(@Valid @RequestBody GroupDTO group) {

        groupRepository.save(new Group_(null,group.group_name(),group.sort(),new Date().getTime(),null,null));

    }
/*
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/update/{gid}")
    void update(@Valid @RequestBody Group group, @PathVariable Integer gid){
        groupRepository.updateGroup(gid, group.result(), group.amount(), group.status());
    }

 */

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/delete/{gid}")
    void delete(@PathVariable Long gid){
        groupRepository.delete(groupRepository.findById(gid).get());
    }
}
