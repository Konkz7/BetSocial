package com.example.World.Security;

import com.example.World.Users.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.World.Users.User_;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;
    private final ObjectMapper objectMapper;

    public CustomAuthenticationSuccessHandler(UserService userService, ObjectMapper objectMapper) {
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        // Example: Log the user details
        HttpSession session = request.getSession();

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();


        if(session.getAttribute("userId") != null){
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            throw new RuntimeException("User is already logged in.");
        }


        userService.changeStatus(user.getUserId(), true);
        userService.notifyGroupsOnStatus(user.getUserId(), true);

        session.setAttribute("userId",user.getUserId());
        System.out.println("User " + authentication.getName() + " has logged in.");


        // One JSON object, written through Jackson. This used to be two separate
        // write() calls, which concatenated into
        //     {"message":"Login successful!"}{"userId":"2"}
        // - not valid JSON, so any client that tried to parse the body failed.
        // The response also never declared its content type.
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(),
                Map.of("message", "Login successful!", "userId", user.getUserId()));
        response.getWriter().flush();
    }

}
