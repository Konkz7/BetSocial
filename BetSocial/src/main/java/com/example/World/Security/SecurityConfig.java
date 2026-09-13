package com.example.World.Security;

import com.example.World.External.WebSocket.HandshakeTicketFilter;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
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
    private final HandshakeTicketFilter handshakeTicketFilter;

    SecurityConfig(UserService userService, CustomLogoutSuccessHandler customLogoutSuccessHandler, CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler,
                   RateLimiter rateLimiter, LoginRateLimitFilter loginRateLimitFilter,
                   HandshakeTicketFilter handshakeTicketFilter){
        this.userService = userService;
        this.customLogoutSuccessHandler = customLogoutSuccessHandler;
        this.customAuthenticationSuccessHandler = customAuthenticationSuccessHandler;
        this.rateLimiter = rateLimiter;
        this.loginRateLimitFilter = loginRateLimitFilter;
        this.handshakeTicketFilter = handshakeTicketFilter;
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
        // After the session has been read, so a handshake that did carry the
        // cookie is already authenticated and its ticket is left unspent. Before
        // authorization, so one that did not can still be let in.
        .addFilterBefore(handshakeTicketFilter, UsernamePasswordAuthenticationFilter.class)
        .authorizeHttpRequests(registry -> {
            registry.requestMatchers("/req/**").permitAll();
            // The host polls this to decide whether to send traffic here, and it
            // has no session. It reports reachability and nothing else - see
            // HealthController, which deliberately returns no detail.
            registry.requestMatchers(HttpMethod.GET, "/health").permitAll();
            // Opened for the phone's browser, which has no session cookie - the
            // one-time token in the URL is the authorisation instead. Exactly
            // this path and this method: /api/users/my-data itself stays behind
            // the session, and a wildcard here would open it too.
            registry.requestMatchers(HttpMethod.GET, "/api/users/my-data/download").permitAll();
            // WebSocket identity is derived from the authenticated principal, not
            // from a client header. The handshake proves who it is with the
            // session cookie, or - when React Native does not send one - with a
            // one-time ticket, which HandshakeTicketFilter turns into the same
            // authentication a cookie would have produced.
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
        })
        .exceptionHandling(handling -> handling
                // A WebSocket handshake that is not signed in was being answered
                // with formLogin's 302 to /login. A redirect is not something an
                // upgrade request can do anything with: the client is waiting for
                // 101, gets a redirect to an HTML page, and neither opens nor
                // fails cleanly - it simply stops, which reads as the socket
                // hanging on "Opening Web Socket...".
                //
                // 401 says the same thing in a form the client can act on, and
                // shows up in its logs. Scoped to the handshake rather than
                // applied everywhere, because the browser-facing pages do want
                // the redirect.
                .defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        new AntPathRequestMatcher("/ws/**")))
        .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(customLogoutSuccessHandler)
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )
        .build();
    }

}
