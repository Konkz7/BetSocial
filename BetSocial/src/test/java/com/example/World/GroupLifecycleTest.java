package com.example.World;

import com.example.World.Groups.GroupService;
import com.example.World.Groups.Group_;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GroupService.createGroup existed but had no endpoint and no caller, so a group
 * could not actually be made - which is why group chat was untestable end to end
 * however group-shaped the schema already was.
 *
 * These cover the permission model the endpoints enforce: any member may add,
 * only an administrator may remove or rename, anyone may leave, the creator is
 * the first administrator, and a group is closed once the last member has gone.
 */
@DisplayName("Group lifecycle")
class GroupLifecycleTest extends AbstractIntegrationTest {

    // See SecurityRegressionTest for the per-class phone_number blocks in use.
    private static final AtomicLong PHONE = new AtomicLong(2_350_000_000_000L);

    @Autowired UserRepository users;
    @Autowired GroupService groups;
    @Autowired PasswordEncoder passwordEncoder;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("the creator is the first administrator")
    void creatorBecomesAdministrator() throws Exception {
        User_ creator = users.save(user());
        User_ member = users.save(user());

        Long gid = createGroup(loginAs(creator), "trip", List.of(member.uid()));

        JsonNode members = mapper.readTree(
                get("/api/groups/members/" + gid, loginAs(creator)).getBody());

        assertThat(members).hasSize(2);
        assertThat(adminFlagOf(members, creator.uid()))
                .as("the creator must be an administrator, or nobody can manage the group")
                .isTrue();
        assertThat(adminFlagOf(members, member.uid())).isFalse();
    }

    @Test
    @DisplayName("any member may add, but only an administrator may remove")
    void addIsOpenRemoveIsRestricted() {
        User_ creator = users.save(user());
        User_ member = users.save(user());
        User_ invitee = users.save(user());

        Long gid = createGroup(loginAs(creator), "five-a-side", List.of(member.uid()));

        assertThat(exchange(HttpMethod.POST, "/api/groups/add-member/" + gid + "/" + invitee.uid(),
                loginAs(member)).getStatusCode())
                .as("an ordinary member may add someone")
                .isEqualTo(HttpStatus.CREATED);

        assertThat(exchange(HttpMethod.DELETE, "/api/groups/remove-member/" + gid + "/" + invitee.uid(),
                loginAs(member)).getStatusCode())
                .as("but an ordinary member may not remove them again")
                .isEqualTo(HttpStatus.FORBIDDEN);

        assertThat(exchange(HttpMethod.DELETE, "/api/groups/remove-member/" + gid + "/" + invitee.uid(),
                loginAs(creator)).getStatusCode())
                .as("an administrator may")
                .isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("only an administrator may rename")
    void onlyAdministratorRenames() {
        User_ creator = users.save(user());
        User_ member = users.save(user());

        Long gid = createGroup(loginAs(creator), "old name", List.of(member.uid()));

        assertThat(rename(gid, loginAs(member), "hijacked").getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        // A name with a space in it, deliberately: it is the ordinary case, and it
        // has to survive the round trip rather than arriving percent-escaped.
        assertThat(rename(gid, loginAs(creator), "new name").getStatusCode())
                .isEqualTo(HttpStatus.OK);

        assertThat(nameOf(gid)).isEqualTo("new name");
    }

    @Test
    @DisplayName("the last administrator leaving promotes the longest-standing member")
    void leavingPromotesASuccessor() throws Exception {
        User_ creator = users.save(user());
        User_ second = users.save(user());
        User_ third = users.save(user());

        Long gid = createGroup(loginAs(creator), "succession", List.of(second.uid(), third.uid()));

        assertThat(exchange(HttpMethod.DELETE, "/api/groups/leave/" + gid, loginAs(creator))
                .getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        JsonNode members = mapper.readTree(
                get("/api/groups/members/" + gid, loginAs(second)).getBody());

        assertThat(members).hasSize(2);
        // Without this the group would be permanently unadministrable: nobody left
        // able to rename it, remove anyone, or close it.
        assertThat(anyAdministrator(members))
                .as("somebody must inherit the administrator role")
                .isTrue();
        assertThat(adminFlagOf(members, second.uid()))
                .as("the longest-standing remaining member takes it")
                .isTrue();
    }

    @Test
    @DisplayName("an emptied group is not deleted out from under its history")
    void emptyGroupSurvives() {
        User_ creator = users.save(user());
        User_ second = users.save(user());

        Long gid = createGroup(loginAs(creator), "abandoned", List.of(second.uid()));

        exchange(HttpMethod.DELETE, "/api/groups/leave/" + gid, loginAs(creator));
        exchange(HttpMethod.DELETE, "/api/groups/leave/" + gid, loginAs(second));

        assertThat(activeMemberCount(gid)).as("nobody is in it any more").isZero();

        // Deleting it here would cascade away the soft-deleted membership rows,
        // which are the only record that either of them was ever in it.
        assertThat(groupExists(gid))
                .as("the group row must survive so the history it holds survives")
                .isTrue();
        assertThat(pastGroupIds(loginAs(second)))
                .as("and both should still be able to see they were in it")
                .contains(gid);
    }

    @Test
    @DisplayName("being removed leaves a record the removed user can see")
    void removalIsVisibleToTheRemovedUser() {
        User_ creator = users.save(user());
        User_ removed = users.save(user());

        Long gid = createGroup(loginAs(creator), "not for you", List.of(removed.uid()));

        assertThat(pastGroupIds(loginAs(removed)))
                .as("nothing to show while they are still a member")
                .doesNotContain(gid);

        exchange(HttpMethod.DELETE, "/api/groups/remove-member/" + gid + "/" + removed.uid(),
                loginAs(creator));

        assertThat(pastGroupIds(loginAs(removed)))
                .as("a hard delete could not be told apart from never having joined")
                .contains(gid);

        // The membership is over, so it is no longer a conversation they can reach.
        assertThat(get("/api/groups/members/" + gid, loginAs(removed)).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(activeMemberCount(gid)).isEqualTo(1);
    }

    @Test
    @DisplayName("a removed member can be added back")
    void removedMemberCanRejoin() {
        User_ creator = users.save(user());
        User_ member = users.save(user());

        Long gid = createGroup(loginAs(creator), "revolving door", List.of(member.uid()));

        exchange(HttpMethod.DELETE, "/api/groups/remove-member/" + gid + "/" + member.uid(),
                loginAs(creator));

        // The soft-deleted row must not block a new one: they hold two memberships
        // for this group now, of which only the second is active.
        assertThat(exchange(HttpMethod.POST, "/api/groups/add-member/" + gid + "/" + member.uid(),
                loginAs(creator)).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        assertThat(activeMemberCount(gid)).isEqualTo(2);
        assertThat(get("/api/groups/members/" + gid, loginAs(member)).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("only an administrator may delete a group, and it takes everything with it")
    void deleteIsRestrictedAndTotal() {
        User_ creator = users.save(user());
        User_ member = users.save(user());

        Long gid = createGroup(loginAs(creator), "doomed", List.of(member.uid()));

        assertThat(exchange(HttpMethod.DELETE, "/api/groups/delete/" + gid, loginAs(member))
                .getStatusCode())
                .as("an ordinary member cannot destroy a conversation")
                .isEqualTo(HttpStatus.FORBIDDEN);

        assertThat(exchange(HttpMethod.DELETE, "/api/groups/delete/" + gid, loginAs(creator))
                .getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(groupExists(gid)).isFalse();
        // V1's foreign keys cascade the delete, so no row is left pointing at a
        // group that has gone.
        assertThat(membershipRowCount(gid))
                .as("membership rows should go with it, soft-deleted ones included")
                .isZero();
    }

    @Test
    @DisplayName("lifecycle operations refuse a direct message")
    void directMessagesAreNotGroups() {
        User_ one = users.save(user());
        User_ two = users.save(user());
        User_ outsider = users.save(user());

        Group_ dm = groups.openDirectConversation(one.uid(), two.uid());

        // A DM has a fixed pair of participants. Adding a third, or leaving half of
        // it, produces a conversation the rest of the code cannot describe.
        assertThat(exchange(HttpMethod.POST,
                "/api/groups/add-member/" + dm.gid() + "/" + outsider.uid(), loginAs(one))
                .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(exchange(HttpMethod.DELETE, "/api/groups/leave/" + dm.gid(), loginAs(one))
                .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("a non-member cannot touch a group")
    void nonMemberIsRefused() {
        User_ creator = users.save(user());
        User_ outsider = users.save(user());

        Long gid = createGroup(loginAs(creator), "private", List.of());
        String session = loginAs(outsider);

        assertThat(get("/api/groups/members/" + gid, session).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exchange(HttpMethod.POST, "/api/groups/add-member/" + gid + "/" + outsider.uid(), session)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exchange(HttpMethod.DELETE, "/api/groups/leave/" + gid, session)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("group size is capped")
    void groupSizeIsCapped() {
        User_ creator = users.save(user());

        // The ids need not exist: the cap is checked before members are resolved,
        // which is the point - the check should not cost 50 lookups to reject.
        List<Long> tooMany = LongStream.rangeClosed(1, GroupService.MAX_GROUP_MEMBERS)
                .boxed().map(l -> l + 900_000L).toList();

        assertThat(postGroup(loginAs(creator), "overfull", tooMany).getStatusCode())
                .as("creator plus %d members exceeds the cap of %d",
                        tooMany.size(), GroupService.MAX_GROUP_MEMBERS)
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("a group cannot be created without a name")
    void nameIsRequired() {
        User_ creator = users.save(user());

        assertThat(postGroup(loginAs(creator), "   ", List.of()).getStatusCode())
                .isIn(HttpStatus.BAD_REQUEST, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    // --- helpers ----------------------------------------------------------

    private Long createGroup(String session, String name, List<Long> members) {
        ResponseEntity<String> response = postGroup(session, name, members);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        try {
            return mapper.readTree(response.getBody()).get("gid").asLong();
        } catch (Exception e) {
            throw new IllegalStateException("could not read the created group", e);
        }
    }

    private ResponseEntity<String> rename(Long gid, String session, String name) {
        return jsonRequest("/api/groups/rename/" + gid, HttpMethod.PUT, session,
                java.util.Map.of("group_name", name));
    }

    private ResponseEntity<String> jsonRequest(String path, HttpMethod method, String session,
                                               Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, session);

        String json;
        try {
            json = mapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        return rest.exchange(path, method, new HttpEntity<>(json, headers), String.class);
    }

    private ResponseEntity<String> postGroup(String session, String name, List<Long> members) {
        return jsonRequest("/api/groups/create", HttpMethod.POST, session,
                java.util.Map.of("group_name", name, "members", members));
    }

    private boolean adminFlagOf(JsonNode members, Long uid) {
        for (JsonNode row : members) {
            if (row.get("uid").asLong() == uid) {
                return row.get("administrator").asBoolean();
            }
        }
        throw new AssertionError("user " + uid + " is not in the member list");
    }

    private boolean anyAdministrator(JsonNode members) {
        for (JsonNode row : members) {
            if (row.get("administrator").asBoolean()) {
                return true;
            }
        }
        return false;
    }

    private boolean groupExists(Long gid) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM group_ WHERE gid = ?", Integer.class, gid) > 0;
    }

    private int activeMemberCount(Long gid) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM groupuser_ WHERE gid = ? AND deleted_at IS NULL",
                Integer.class, gid);
    }

    private int membershipRowCount(Long gid) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM groupuser_ WHERE gid = ?", Integer.class, gid);
    }

    private List<Long> pastGroupIds(String session) {
        try {
            JsonNode rows = mapper.readTree(get("/api/groups/past-groups", session).getBody());
            List<Long> gids = new java.util.ArrayList<>();
            rows.forEach(row -> gids.add(row.get("gid").asLong()));
            return gids;
        } catch (Exception e) {
            throw new IllegalStateException("could not read past groups", e);
        }
    }

    private String nameOf(Long gid) {
        return jdbc.queryForObject("SELECT group_name FROM group_ WHERE gid = ?", String.class, gid);
    }

    private String loginAs(User_ user) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", user.user_name());
        form.add("password", "password");

        ResponseEntity<String> response =
                rest.postForEntity("/login", new HttpEntity<>(form, headers), String.class);

        assertThat(response.getStatusCode())
                .as("login should succeed for %s", user.user_name())
                .isEqualTo(HttpStatus.OK);

        return response.getHeaders().getFirst(HttpHeaders.SET_COOKIE).split(";")[0];
    }

    private ResponseEntity<String> get(String path, String session) {
        return exchange(HttpMethod.GET, path, session);
    }

    private ResponseEntity<String> exchange(HttpMethod method, String path, String session) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, session);
        return rest.exchange(path, method, new HttpEntity<>(headers), String.class);
    }

    private User_ user() {
        long seq = PHONE.incrementAndGet();
        String name = "lifecycle-" + seq;
        return new User_(null, name, name + "@example.test", passwordEncoder.encode("password"),
                "+" + seq, null, true, "", null, new Date().getTime(), null,
                0, null, "offline", null, 0.0, null);
    }
}
