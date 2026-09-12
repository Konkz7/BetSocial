package com.example.World.Media;

import com.example.World.Users.UserRepository;
import com.example.World.Users.User_;
import com.example.World.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Who is allowed to be told they are somebody.
 *
 * The token this endpoint returns is the thing the Storage rules will trust, so
 * the interesting tests are not that it produces one - they are that it refuses
 * to, and that the identity in it is the session's rather than the caller's.
 *
 * The minting itself is mocked. Signing needs a real service-account key, which
 * has no place in a test run, and the part worth protecting is the authorisation
 * around the call rather than Firebase's ability to sign a JWT.
 */
@DisplayName("Upload tokens")
class MediaTokenTest extends AbstractIntegrationTest {

    // See SecurityRegressionTest for the per-class phone_number blocks in use.
    private static final AtomicLong PHONE = new AtomicLong(2_366_000_000_000L);

    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;

    @MockitoBean UploadTokens uploadTokens;

    @Test
    @DisplayName("a signed-in person gets a token")
    void issuesATokenToAMember() {
        User_ person = user();
        when(uploadTokens.forUser(person.uid())).thenReturn("signed-assertion");

        ResponseEntity<String> response = mint(loginAs(person));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("signed-assertion")
                .as("the client needs to know how long it has to exchange it")
                .contains("3600");
    }

    @Test
    @DisplayName("nobody signed in gets nothing")
    void refusesAnonymousCallers() {
        ResponseEntity<String> response = mint(null);

        assertThat(response.getStatusCode())
                .as("a token handed out without a session is an identity handed "
                        + "to anybody who asks, which is worse than no rule at all")
                .isNotEqualTo(HttpStatus.OK);

        verify(uploadTokens, never()).forUser(anyLong());
    }

    @Test
    @DisplayName("the identity is the session's, not the caller's")
    void ignoresAnyIdentityInTheRequest() {
        User_ person = user();
        User_ somebodyElse = user();
        when(uploadTokens.forUser(anyLong())).thenReturn("signed-assertion");

        // Whatever a caller supplies, the uid in the token has to come from the
        // session. Reading it from the request would let anybody mint a token for
        // anybody, and the rules would then be pinning files to an identity the
        // caller chose - which is no pinning at all.
        post("/api/media/token?uid=" + somebodyElse.uid(), loginAs(person));

        verify(uploadTokens).forUser(person.uid());
        verify(uploadTokens, never()).forUser(somebodyElse.uid());
    }

    @Test
    @DisplayName("asking over and over is limited")
    void isRateLimited() {
        when(uploadTokens.forUser(anyLong())).thenReturn("signed-assertion");
        String session = loginAs(user());

        // Limits.UPLOAD_TOKENS allows 60 an hour; 80 attempts must not all pass.
        long refused = IntStream.range(0, 80)
                .mapToObj(i -> mint(session))
                .filter(response -> response.getStatusCode().value() == 429)
                .count();

        assertThat(refused)
                .as("an unlimited supply of identities is a way to farm write "
                        + "access to the bucket")
                .isPositive();
    }

    // --- helpers ----------------------------------------------------------

    private User_ user() {
        long seq = PHONE.incrementAndGet();
        String name = "mediatoken-" + seq;
        return users.save(new User_(null, name, name + "@example.test",
                passwordEncoder.encode("password"),
                "+" + seq, null, true, "", null, new Date().getTime(), null,
                0, null, "offline", null, 0.0, null));
    }

    private String loginAs(User_ user) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", user.user_name());
        form.add("password", "password");
        ResponseEntity<String> response =
                rest.postForEntity("/login", new HttpEntity<>(form, headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getHeaders().getFirst(HttpHeaders.SET_COOKIE).split(";")[0];
    }

    private ResponseEntity<String> mint(String session) {
        return post("/api/media/token", session);
    }

    private ResponseEntity<String> post(String path, String session) {
        HttpHeaders headers = new HttpHeaders();
        if (session != null) {
            headers.add(HttpHeaders.COOKIE, session);
        }
        return rest.exchange(path, HttpMethod.POST, new HttpEntity<>(headers), String.class);
    }
}
