package com.example.World.Security;

import com.example.World.Users.User_;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        // Example: Log the user details
        HttpSession session = request.getSession();

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

        if(session.getAttribute("userId") != null){
            throw new RuntimeException("User is already logged in.");
        }

        session.setAttribute("userId",user.getUserId());
        System.out.println("User " + authentication.getName() + " has logged in.");

        // Example: Redirect to a specific URL or return a response
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write("{\"message\":\"Login successful!\"}");
        response.getWriter().flush();
    }

}
