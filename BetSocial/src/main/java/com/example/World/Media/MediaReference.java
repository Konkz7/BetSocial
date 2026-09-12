package com.example.World.Media;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Checks that a piece of media is ours.
 *
 * Uploads go from the device straight to Firebase Storage and only the resulting
 * URL reaches the server, which stored it in thread_.media or message_.description
 * without looking at it. Any string was accepted, so a thread's "image" could be
 * a URL to anywhere - and the client renders it, which makes that worth having.
 * Pointing it at somebody's own server logs the address of every person who
 * scrolls past; pointing it somewhere worse is no harder.
 *
 * Nothing here can enforce a size or a file type: the bytes never pass through
 * this application. Those limits live in the client, and the only place they can
 * actually be enforced is the Storage rules on the bucket - see the README.
 * What this can do is insist the URL names an object in our own bucket, under a
 * prefix we use.
 */
@Component
public class MediaReference {

    private static final Logger log = LoggerFactory.getLogger(MediaReference.class);

    /** The host every Firebase Storage download URL is served from. */
    private static final String STORAGE_HOST = "firebasestorage.googleapis.com";

    /**
     * The prefixes FBStorageService uploads under. Anything outside them is not
     * something this application put there, even if the bucket is right.
     */
    private static final List<String> ALLOWED_PREFIXES =
            List.of("images/", "videos/", "profile_pictures/");

    /**
     * text columns, but a URL this long is not a URL somebody typed. A cap keeps
     * an unbounded string out of a row that is read on every feed request.
     */
    private static final int MAX_LENGTH = 1_000;

    private final String bucket;

    public MediaReference(@Value("${firebase.storage-bucket:}") String bucket) {
        this.bucket = bucket;
    }

    /**
     * Passes a reference through, or refuses it.
     *
     * Null and blank are allowed: most threads have no media, and the column is
     * nullable. This is about what a non-empty value is permitted to be.
     */
    public String require(String media) {
        if (media == null || media.isBlank()) {
            return media;
        }

        // Without a bucket there is nothing to compare against, so refusing would
        // reject every upload the app makes rather than only the ones it did not.
        // Accepting unchecked is the lesser wrong of the two - but it does mean
        // this whole class is inert until the bucket is set, which is why startup
        // says so out loud.
        if (bucket.isBlank()) {
            return media;
        }

        if (media.length() > MAX_LENGTH) {
            throw refuse();
        }

        if (objectPathOf(media).isEmpty()) {
            throw refuse();
        }

        return media;
    }

    /**
     * Says, once, that media is not being checked.
     *
     * A warning at startup rather than a line per request: the person who can fix
     * it is reading the log when the application comes up, and one every time
     * somebody posts a photo would be ignored within a day.
     */
    @PostConstruct
    void warnIfUnconfigured() {
        if (bucket.isBlank()) {
            log.warn("firebase.storage-bucket is not set: media references are accepted "
                    + "without checking, and files are not deleted when their content is "
                    + "removed. Set FIREBASE_STORAGE_BUCKET before this is public.");
        }
    }

    /**
     * The object's path inside the bucket, if this URL names one of ours.
     *
     * Empty for anything else, which is what makes it usable both as the
     * validator above and as the lookup for deleting the object later - one
     * definition of "ours", rather than two that can disagree.
     */
    public Optional<String> objectPathOf(String media) {
        if (media == null || media.isBlank() || bucket.isBlank()) {
            return Optional.empty();
        }

        URI uri;
        try {
            uri = new URI(media);
        } catch (URISyntaxException notAUri) {
            return Optional.empty();
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())
                || !STORAGE_HOST.equalsIgnoreCase(uri.getHost())) {
            return Optional.empty();
        }

        // https://firebasestorage.googleapis.com/v0/b/<bucket>/o/<url-encoded path>
        String expectedPrefix = "/v0/b/" + bucket + "/o/";
        String path = uri.getRawPath();
        if (path == null || !path.startsWith(expectedPrefix)) {
            return Optional.empty();
        }

        // The object path is percent-encoded in the URL - "images/x.jpg" arrives
        // as "images%2Fx.jpg" - so it has to be decoded before the prefix can be
        // recognised, and before it can be handed to the bucket.
        String objectPath = URLDecoder.decode(
                path.substring(expectedPrefix.length()), StandardCharsets.UTF_8);

        // No traversal. The bucket API treats the path as a key rather than a
        // filesystem path, so ".." is not the hazard it would be on disk - but a
        // path containing one did not come from us either way.
        if (objectPath.contains("..")) {
            return Optional.empty();
        }

        boolean known = ALLOWED_PREFIXES.stream().anyMatch(objectPath::startsWith);
        return known ? Optional.of(objectPath) : Optional.empty();
    }

    private static ResponseStatusException refuse() {
        // Deliberately not explaining which rule was broken. A client of ours
        // only ever sends URLs produced by its own upload, so anything reaching
        // this is either a bug or somebody trying things.
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "That media reference is not one of ours");
    }
}
