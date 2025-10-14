package com.example.World.Users;

import com.example.World.External.Firebase.AuthService;
import com.example.World.Groups.GroupService;
import com.example.World.Groups.GroupUserRepository;
import com.example.World.Groups.Groupuser_;
import com.example.World.Messages.GroupStatus;
import com.example.World.Security.CustomUserDetails;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.example.World.Users.UserRole.*;

@Service
public class UserService implements UserDetailsService {

    private  final AuthService authService;
    private final UserRepository userRepository;
    private final GroupService groupService;
    private final SimpMessageSendingOperations messageOperations;


    UserService(AuthService authService, UserRepository userRepository, GroupService groupService, SimpMessageSendingOperations messageOperations){

        this.authService = authService;
        this.userRepository = userRepository;
        this.groupService = groupService;
        this.messageOperations = messageOperations;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User_> user = userRepository.findByUsername(username);
        if(user.isPresent()){
            User_ user_obj = user.get();

            if (user_obj.deleted_at() != null) {
                throw new UsernameNotFoundException("User has been deleted");
            }

            List<GrantedAuthority> authorities = getGrantedAuthorities(user_obj);

            // Create CustomUserDetails object directly
            CustomUserDetails customUserDetails = new CustomUserDetails(
                    user_obj.user_name(),
                    user_obj.pass_word(),
                    // You can provide roles or authorities here if necessary
                    authorities
            );

            // Set additional properties like userId
            customUserDetails.setUserId(user_obj.uid());

            return customUserDetails;
        }

        throw new UsernameNotFoundException("User not found");
    }

    private List<GrantedAuthority> getGrantedAuthorities(User_ user_obj) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if(user_obj.user_role() == USER.toInt()){
            authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        } else if (user_obj.user_role() == SUPERUSER.toInt()){
            authorities = List.of(new SimpleGrantedAuthority("ROLE_SUPERUSER"));
        } else if (user_obj.user_role() == ADMIN.toInt()){
            authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"),new SimpleGrantedAuthority("ROLE_SUPERUSER"));
        }
        return authorities;
    }


    public boolean checkVerificationEmail(String email) throws Exception {
        return authService.checkEmailVerified(email);
    }




    public boolean uniqueFBNLog(Long uid,String token){
        for(User_ u : userRepository.findByFBNToken(token)){
            userRepository.saveFBNtoken(u.uid(),null);
        }

        return userRepository.saveFBNtoken(uid, token) == 1;

    }

    public void changeStatus(Long uid,boolean online){
        if(online){
            userRepository.changeStatus(uid,"online");
        }else{
            userRepository.changeStatus(uid,"offline");
        }
    }

    public void notifyGroupsOnStatus(Long uid, boolean online){
        List<Groupuser_> groupuserList = groupService.getGroupProfiles(uid);
        for(Groupuser_ gu : groupuserList){
            GroupStatus gp = new GroupStatus(uid, gu.gid(), online, false);
            messageOperations.convertAndSend("/topic/chat/" + gu.gid(), gp);
        }
    }


}
