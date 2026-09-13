package com.example.World.Security;

import com.example.World.RateLimit.Limits;
import com.example.World.RateLimit.LoginRateLimitFilter;
import com.example.World.RateLimit.RateLimiter;
import com.example.World.Users.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserService userService;
    private final CustomLogoutSuccessHandler customLogoutSuccessHandler;
    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;
    private final RateLimiter rateLimiter;
    private final LoginRateLimitFilter loginRateLimitFilter;

    SecurityConfig(UserService userService, CustomLogoutSuccessHandler customLogoutSuccessHandler, CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler,
                   RateLimiter rateLimiter, LoginRateLimitFilter loginRateLimitFilter){
        this.userService = userService;
        this.customLogoutSuccessHandler = customLogoutSuccessHandler;
        this.customAuthenticationSuccessHandler = customAuthenticationSuccessHandler;
        this.rateLimiter = rateLimiter;
        this.loginRateLimitFilter = loginRateLimitFilter;
    }

    @Bean
    public UserDetailsService userDetailsService(){
        return userService;
    }

    @Bean
    public AuthenticationProvider authenticationProvider(){
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
        .csrf(AbstractHttpConfigurer::disable)
        // Before the login filter on purpose: the point is to stop the password
        // being checked at all, not to notice afterwards that it was.
        .addFilterBefore(loginRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
        .authorizeHttpRequests(registry -> {
            registry.requestMatchers("/req/**").permitAll();
            // Opened for the phone's browser, which has no session cookie - the
            // one-time token in the URL is the authorisation instead. Exactly
            // this path and this method: /api/users/my-data itself stays behind
            // the session, and a wildcard here would open it too.
            registry.requestMatchers(HttpMethod.GET, "/api/users/my-data/download").permitAll();
            // The STOMP handshake must carry the session cookie: WebSocket identity
            // is now derived from the authenticated principal, not a client header.
            registry.requestMatchers("/ws/**").authenticated();
            // ROLE_SUPERUSER / ROLE_ADMIN are the only elevated roles UserService grants
            // (see getGrantedAuthorities). "IMAGE" and "TEXT" were never issued to anyone,
            // so these rules denied every user - including admins - and made the bets and
            // predictions APIs unreachable.
            registry.requestMatchers("/superusers/**").hasAnyRole("SUPERUSER","ADMIN");
            registry.requestMatchers("/admin/**").hasRole("ADMIN");
            registry.requestMatchers("/api/bets/**").authenticated();
            registry.requestMatchers("/api/predictions/**").authenticated();

            registry.anyRequest().authenticated();
        })
        .formLogin(httpform -> {
            httpform.loginProcessingUrl("/login")
                    .successHandler(customAuthenticationSuccessHandler)
                    .failureHandler(new AuthenticationFailureHandler() {
                        @Override
                        public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
                            // Spends one of this address's allowance. Only
                            // failures count: LoginRateLimitFilter refuses once
                            // the allowance is gone, so somebody signing in
                            // successfully all day is never limited.
                            try {
                                rateLimiter.require(LoginRateLimitFilter.keyFor(request), Limits.LOGIN);
                            } catch (ResponseStatusException alreadySpent) {
                                // The allowance was already empty. Nothing to
                                // record, and the answer is the same either way -
                                // this must not replace the 401 with a 500.
                            }

                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\": \"Invalid username or password\"}");
                            response.getWriter().flush();
                        }
                    });
        }).logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(customLogoutSuccessHandler)
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )
        .build();
    }

}
