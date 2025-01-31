package com.example.World.Groups;



import com.example.World.Users.User_;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


public interface GroupUserRepository extends ListCrudRepository<GroupUser_,Long> {


    @Query("SELECT * FROM GroupUser_ WHERE uid= :uid")
    List<GroupUser_> findByUid(@Param("uid") Long uid);


}
