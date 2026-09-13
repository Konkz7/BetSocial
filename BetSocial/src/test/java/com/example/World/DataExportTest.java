package com.example.World;

import com.example.World.Users.UserRepository;
import com.example.World.Users.User_;
import com.example.World.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Taking your data away with you.
 *
 * The export is served to the phone's browser rather than the app, because the
 * app has nowhere to put a file that its owner can find again. That means the
 * download URL carries its own permission instead of a session cookie - so what
 * matters here is not that the file arrives, it is that the link cannot be
 * reused, cannot be guessed, and cannot be pointed at anybody else.
 */
@DisplayName("Data export")
class DataExportTest extends AbstractIntegrationTest {

    // See SecurityRegressionTest for the per-class phone_number blocks in use.
    private static final AtomicLong PHONE = new AtomicLong(2_367_000_000_000L);

    private static final String LINK = "/api/users/my-data/link";
    private static final String DOWNLOAD = "/api/users/my-data/download?token=";

    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("arrives as a file, named, with the caller's own data in it")
    void downloadsAsAFile() throws Exception {
        User_ person = user();

        ResponseEntity<String> file = rest.getForEntity(DOWNLOAD + tokenFor(person), String.class);

        assertThat(file.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(file.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .as("without this the browser renders the json instead of saving it, "
                        + "which is the whole reason for going via the browser")
                .startsWith("attachment;")
                .contains("betsocial-my-data-");

        JsonNode body = mapper.readTree(file.getBody());
        assertThat(body.path("profile").path("email").asText()).isEqualTo(person.email());
        assertThat(body.has("threads")).isTrue();
        assertThat(body.has("messages")).isTrue();
    }

    @Test
    @DisplayName("a link works once")
    void tokensAreSingleUse() {
        String token = tokenFor(user());

        assertThat(rest.getForEntity(DOWNLOAD + token, String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        assertThat(rest.getForEntity(DOWNLOAD + token, String.class).getStatusCode())
                .as("a link that keeps working is one that keeps being in browser "
                        + "history, and this one returns an email address and private messages")
                .isNotEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("a token nobody issued is refused")
    void refusesUnknownTokens() {
        assertThat(rest.getForEntity(DOWNLOAD + "not-a-real-token", String.class).getStatusCode())
                .isNotEqualTo(HttpStatus.OK);

        assertThat(rest.getForEntity("/api/users/my-data/download", String.class).getStatusCode())
                .as("no token at all is not the same as a blank one being accepted")
                .isNotEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("the download path is open, but nothing else under my-data is")
    void opensOnlyTheDownloadPath() {
        ResponseEntity<String> withoutSession = rest.getForEntity("/api/users/my-data", String.class);

        assertThat(withoutSession.getStatusCode())
                .as("the permitAll rule is written for one path and one method; a "
                        + "wildcard would have opened the session-backed export too")
                .isNotEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("a link cannot be asked for without signing in")
    void refusesAnonymousLinkRequests() {
        ResponseEntity<String> response = rest.exchange(LINK, HttpMethod.POST,
                new HttpEntity<>(new HttpHeaders()), String.class);

        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("a link returns its own owner's data, not whoever opens it")
    void tokensAreBoundToTheirOwner() throws Exception {
        User_ owner = user();
        User_ somebodyElse = user();

        // Downloaded with no session at all, and while a different person is the
        // only one signed in - the token names the user, so neither can change
        // whose data comes back.
        loginAs(somebodyElse);
        ResponseEntity<String> file = rest.getForEntity(DOWNLOAD + tokenFor(owner), String.class);

        JsonNode body = mapper.readTree(file.getBody());
        assertThat(body.path("profile").path("email").asText()).isEqualTo(owner.email());
        assertThat(body.path("profile").path("email").asText()).isNotEqualTo(somebodyElse.email());
    }

    // --- helpers ----------------------------------------------------------

    private String tokenFor(User_ person) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, loginAs(person));
        ResponseEntity<String> response =
                rest.exchange(LINK, HttpMethod.POST, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        try {
            return mapper.readTree(response.getBody()).path("token").asText();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private User_ user() {
        long seq = PHONE.incrementAndGet();
        String name = "export-" + seq;
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
}
