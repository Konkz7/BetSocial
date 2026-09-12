package com.example.World.Media;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * What counts as one of our files.
 *
 * thread_.media and a media message's description took any string the client
 * sent, and the app renders whatever is in them. A thread whose "image" is a URL
 * to somebody's own server logs the address of every person who scrolls past it;
 * in a private conversation it reports who opened the chat and when.
 *
 * A plain unit test: this is string parsing, and running it against a database
 * would make it slower without testing anything more.
 */
@DisplayName("Media references")
class MediaReferenceTest {

    private static final String BUCKET = "betsocial-test.appspot.com";

    private final MediaReference media = new MediaReference(BUCKET);

    private static String ours(String objectPath) {
        return in(BUCKET, objectPath);
    }

    private static String in(String bucket, String objectPath) {
        return "https://firebasestorage.googleapis.com/v0/b/" + bucket + "/o/"
                + objectPath.replace("/", "%2F") + "?alt=media&token=abc";
    }

    @Test
    @DisplayName("accepts a URL for an object in our bucket")
    void acceptsOurOwnUrls() {
        assertThatCode(() -> {
            media.require(ours("images/abc.jpg"));
            media.require(ours("videos/abc.mp4"));
            media.require(ours("profile_pictures/7.jpg"));
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("no media is fine - most threads have none")
    void allowsAbsence() {
        assertThatCode(() -> {
            media.require(null);
            media.require("");
            media.require("   ");
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("refuses somebody else's server")
    void refusesForeignHosts() {
        assertThatThrownBy(() -> media.require("https://example.com/tracker.gif"))
                .as("this is the whole reason the check exists")
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("refuses another bucket on the right host")
    void refusesOtherBuckets() {
        assertThatThrownBy(() -> media.require(
                "https://firebasestorage.googleapis.com/v0/b/someone-else.appspot.com/o/images%2Fx.jpg"))
                .as("the host being right is not the same as the bucket being ours")
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("refuses a prefix we never upload to")
    void refusesUnknownPrefixes() {
        assertThatThrownBy(() -> media.require(ours("exports/private.json")))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("refuses anything that is not https")
    void refusesOtherSchemes() {
        assertThatThrownBy(() -> media.require("http://firebasestorage.googleapis.com/v0/b/"
                + BUCKET + "/o/images%2Fx.jpg"))
                .isInstanceOf(ResponseStatusException.class);

        assertThatThrownBy(() -> media.require("javascript:alert(1)"))
                .isInstanceOf(ResponseStatusException.class);

        assertThatThrownBy(() -> media.require("file:///etc/passwd"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("refuses a host that merely ends with ours")
    void refusesLookalikeHosts() {
        assertThatThrownBy(() -> media.require(
                "https://firebasestorage.googleapis.com.evil.test/v0/b/" + BUCKET + "/o/images%2Fx.jpg"))
                .as("a suffix match would accept this, which is the point of matching the host exactly")
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("refuses traversal in the object path")
    void refusesTraversal() {
        assertThatThrownBy(() -> media.require(ours("images/../profile_pictures/1.jpg")))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("refuses something absurdly long")
    void refusesOverlongValues() {
        assertThatThrownBy(() -> media.require(ours("images/" + "x".repeat(2_000) + ".jpg")))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("refuses a string that is not a URL at all")
    void refusesNonUrls() {
        assertThatThrownBy(() -> media.require("not a url"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("gives back the object path, decoded, for deleting it later")
    void extractsTheObjectPath() {
        assertThat(media.objectPathOf(ours("images/abc.jpg")))
                .as("one definition of \"ours\", used by both the check and the delete")
                .contains("images/abc.jpg");

        assertThat(media.objectPathOf("https://example.com/x.jpg")).isEmpty();
    }

    @Test
    @DisplayName("with no bucket configured, nothing is recognised as ours")
    void withoutABucketNothingIsOurs() {
        MediaReference unconfigured = new MediaReference("");

        assertThat(unconfigured.objectPathOf(ours("images/abc.jpg")))
                .as("nothing can be deleted when we do not know which bucket to look in")
                .isEmpty();
    }

    @Test
    @DisplayName("with no bucket configured, everything is let through")
    void withoutABucketNothingIsRefused() {
        MediaReference unconfigured = new MediaReference("");

        // Deliberate, and the less bad of two wrongs: with nothing to compare
        // against, refusing would reject every upload the app makes rather than
        // only the ones it did not. It does mean the check is inert until the
        // bucket is set, which is what the startup warning is for - and this test
        // is here so that trade-off is visible rather than discovered.
        assertThatCode(() -> {
            unconfigured.require(ours("images/abc.jpg"));
            unconfigured.require("https://example.com/tracker.gif");
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("accepts the other spelling of our own bucket")
    void acceptsTheCounterpartName() {
        String otherSpelling = in("betsocial-test.firebasestorage.app", "images/abc.jpg");

        // Configured as .appspot.com, uploaded to .firebasestorage.app. Only one
        // of the two exists for a given project, so whichever is in the property
        // and whichever the client happens to use should not decide whether
        // photos work - the failure is otherwise "the app uploaded it and then
        // the server said it was not ours", which reads as a bug in neither.
        assertThatCode(() -> media.require(otherSpelling)).doesNotThrowAnyException();
        assertThat(media.objectPathOf(otherSpelling)).contains("images/abc.jpg");

        MediaReference configuredTheOtherWay = new MediaReference("betsocial-test.firebasestorage.app");
        assertThatCode(() -> configuredTheOtherWay.require(ours("images/abc.jpg")))
                .as("and the same in reverse")
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("the counterpart is still only ours, not anyone with the same suffix")
    void theCounterpartIsStillOurProject() {
        assertThatThrownBy(() -> media.require(in("someone-else.firebasestorage.app", "images/x.jpg")))
                .as("accepting an alias must not widen this to every project on the host")
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("tolerates a property with whitespace around it")
    void trimsTheConfiguredValue() {
        MediaReference padded = new MediaReference("  " + BUCKET + "  ");

        assertThatCode(() -> padded.require(ours("images/abc.jpg")))
                .as("a trailing space in an env var should not silently disable the check")
                .doesNotThrowAnyException();
    }
}
