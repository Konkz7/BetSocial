package com.example.World.Security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(CustomLogoutSuccessHandler.class);

    private final UserService userService;

    public CustomLogoutSuccessHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request,
                                HttpServletResponse response,
                                org.springframework.security.core.Authentication authentication)
            throws IOException, ServletException {

        // Null when there was nobody signed in, which is the common case rather
        // than the odd one: the login screen posts /logout every time it gains
        // focus, so most calls here arrive with no session at all. Dereferencing
        // it wrote a NullPointerException and a full stack trace to the log on
        // every visit to that screen, and answered the request with a 500.
        //
        // Logging out when you are not logged in is not an error. It is already
        // true.
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails user)) {
            respondOk(response);
            return;
        }

        HttpSession session = request.getSession(false);

        if (session != null) {
            Long uid = user.getUserId();
            log.debug("Signing out user {}", uid);

            userService.changeStatus(uid,false);
            userService.notifyGroupsOnStatus(uid, false);

            session.invalidate();
        }

        respondOk(response);
    }

    private static void respondOk(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"Logout successful!\"}");
        response.getWriter().flush();
    }
}