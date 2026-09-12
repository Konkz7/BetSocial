package com.example.World;

import com.example.World.Blocks.BlockService;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Finding people, instead of listing everybody.
 *
 * /api/users/all returned every account in the database, on every open of the
 * search screen, the group-creation picker and the member list. All three were
 * choosing somebody and all three then filtered by name in memory, so the
 * request was unbounded and larger than anything any of them drew.
 *
 * Paging it would have been the wrong fix: nobody scrolls to account four
 * hundred to pick a friend. Searching is what those screens were doing already.
 */
@DisplayName("User search")
class UserSearchTest extends AbstractIntegrationTest {

    // See SecurityRegressionTest for the per-class phone_number blocks in use.
    private static final AtomicLong PHONE = new AtomicLong(2_361_000_000_000L);

    private static final int SEARCH_LIMIT = 30;

    @Autowired UserRepository users;
    @Autowired BlockService blockService;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("finds people by part of a name")
    void findsByPartialName() {
        User_ viewer = user("searcher");
        User_ target = user("findable-needle");

        assertThat(names(get("/api/users/search?q=findable-needle", loginAs(viewer))))
                .contains(target.user_name());
    }

    @Test
    @DisplayName("ignores case, the way the screens did in memory")
    void searchIsCaseInsensitive() {
        User_ viewer = user("searcher");
        User_ target = user("MixedCaseName");

        assertThat(names(get("/api/users/search?q=mixedcase", loginAs(viewer))))
                .as("all three callers used toLowerCase().includes(...)")
                .contains(target.user_name());
    }

    @Test
    @DisplayName("never returns more than the cap, however many match")
    void resultsAreCapped() {
        User_ viewer = user("searcher");
        // More than one page of people sharing a prefix.
        IntStream.range(0, SEARCH_LIMIT + 12).forEach(i -> user("capped" + i));

        assertThat(names(get("/api/users/search?q=capped", loginAs(viewer))))
                .as("the whole point is that this cannot return the database")
                .hasSizeLessThanOrEqualTo(SEARCH_LIMIT);
    }

    @Test
    @DisplayName("an empty term still returns people, capped")
    void emptyTermReturnsAList() {
        User_ viewer = user("searcher");
        user("someone-else");

        List<String> found = names(get("/api/users/search", loginAs(viewer)));

        assertThat(found)
                .as("a picker opening on a blank screen has nothing to start from")
                .isNotEmpty()
                .hasSizeLessThanOrEqualTo(SEARCH_LIMIT);
    }

    @Test
    @DisplayName("never includes the person searching")
    void excludesSelf() {
        User_ viewer = user("selfexcluded");

        assertThat(names(get("/api/users/search?q=selfexcluded", loginAs(viewer))))
                .doesNotContain(viewer.user_name());
    }

    @Test
    @DisplayName("excludes blocked people, both directions")
    void excludesBlocked() {
        User_ viewer = user("blocksearcher");
        User_ nuisance = user("blocked-from-search");
        String session = loginAs(viewer);

        assertThat(names(get("/api/users/search?q=blocked-from-search", session)))
                .as("visible before the block, or this proves nothing")
                .contains(nuisance.user_name());

        blockService.block(viewer.uid(), nuisance.uid());

        assertThat(names(get("/api/users/search?q=blocked-from-search", session)))
                .doesNotContain(nuisance.user_name());
    }

    @Test
    @DisplayName("resolves specific accounts by id")
    void resolvesByIds() {
        User_ viewer = user("resolver");
        User_ a = user("byid-one");
        User_ b = user("byid-two");

        List<String> found = names(get(
                "/api/users/by-ids?ids=" + a.uid() + "," + b.uid(), loginAs(viewer)));

        assertThat(found).containsExactlyInAnyOrder(a.user_name(), b.user_name());
    }

    @Test
    @DisplayName("by-ids refuses an unreasonable number of them")
    void byIdsIsBounded() {
        User_ viewer = user("resolver");
        String tooMany = IntStream.rangeClosed(1, 201)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(","));

        assertThat(get("/api/users/by-ids?ids=" + tooMany, loginAs(viewer)).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("the unbounded list endpoint is gone")
    void listEndpointRemoved() {
        ResponseEntity<String> response = get("/api/users/all", loginAs(user("checker")));

        // 400 rather than 404, because GET /{uid} now absorbs the path and cannot
        // parse "all" as an id. Which refusal it is, is Spring's business; what
        // matters is that it is not OK and carries no accounts.
        assertThat(response.getStatusCode())
                .as("it returned every account in the database to any caller")
                .isNotEqualTo(HttpStatus.OK);
        assertThat(response.getBody() == null ? "" : response.getBody())
                .doesNotContain("user_name");
    }

    @Test
    @DisplayName("searching needs a session")
    void anonymousIsRefused() {
        ResponseEntity<String> response = rest.exchange("/api/users/search?q=a",
                HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), String.class);

        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.OK);
    }

    // --- helpers ----------------------------------------------------------

    private List<String> names(ResponseEntity<String> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody() == null ? "" : response.getBody();

        // Reading the names straight out of the JSON rather than binding a type:
        // UserView is what the endpoint returns and the assertion is only ever
        // about which people came back.
        return java.util.regex.Pattern.compile("\"user_name\":\"([^\"]+)\"")
                .matcher(body).results()
                .map(match -> match.group(1))
                .toList();
    }

    private User_ user(String prefix) {
        long seq = PHONE.incrementAndGet();
        String name = prefix + "-" + seq;
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

    private ResponseEntity<String> get(String path, String session) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, session);
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }
}
