package com.example.World.Security;

import com.example.World.Users.UserRepository;
import com.example.World.Users.UserRole;
import com.example.World.Users.User_;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.example.World.Users.UserRole.*;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    UserService(UserRepository userRepository){
        this.userRepository = userRepository;
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
}
