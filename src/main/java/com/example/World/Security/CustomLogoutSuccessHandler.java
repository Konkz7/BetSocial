package com.example.World.Security;

import com.example.World.Users.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    private final UserService userService;

    public CustomLogoutSuccessHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request,
                                HttpServletResponse response,
                                org.springframework.security.core.Authentication authentication)
            throws IOException, ServletException {

        HttpSession session = request.getSession();

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();


        if (session != null) {
            Long uid = user.getUserId();
            System.out.println("session isnt null" + uid);

            if (uid != null) {
                // Clear FCM token in the DB for this user
                //userService.saveFBNToken(uid, null);
                System.out.println("Cleared FCM token for user: " + uid);
            }

            // Invalidate session
            session.invalidate();
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write("{\"message\":\"Logout successful!\"}");
        response.getWriter().flush();
    }
}