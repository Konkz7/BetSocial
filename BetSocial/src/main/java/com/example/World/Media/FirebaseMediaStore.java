package com.example.World.Media;

import com.google.cloud.storage.Blob;
import com.google.firebase.cloud.StorageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Deletes from the Firebase Storage bucket the client uploads to.
 *
 * The Admin SDK is already here for push notifications and carries
 * google-cloud-storage with it, so this needed no new dependency - only the
 * bucket name, which FirebaseConfig did not previously set because nothing
 * server-side had ever touched storage.
 */
@Component
public class FirebaseMediaStore implements MediaStore {

    private static final Logger log = LoggerFactory.getLogger(FirebaseMediaStore.class);

    private final MediaReference mediaReference;
    private final String bucket;

    public FirebaseMediaStore(MediaReference mediaReference,
                              @Value("${firebase.storage-bucket:}") String bucket) {
        this.mediaReference = mediaReference;
        this.bucket = bucket;
    }

    @Override
    public boolean delete(String media) {
        if (bucket.isBlank()) {
            // Not configured. Said once per attempt at debug rather than warned,
            // because running without a bucket is the normal state locally and a
            // warning nobody can act on is noise.
            log.debug("No storage bucket configured; leaving media in place");
            return false;
        }

        Optional<String> objectPath = mediaReference.objectPathOf(media);
        if (objectPath.isEmpty()) {
            // Not ours, or not a storage URL at all. Rows created before media
            // references were validated can hold anything, so this is expected
            // rather than exceptional.
            return false;
        }

        try {
            Blob blob = StorageClient.getInstance().bucket(bucket).get(objectPath.get());
            if (blob == null) {
                return false;
            }
            boolean deleted = blob.delete();
            if (deleted) {
                log.info("Deleted media object {}", objectPath.get());
            }
            return deleted;
        } catch (Exception e) {
            // Never rethrown. The caller is removing a thread or acting on a
            // report; a storage failure must not undo that. A file left behind is
            // untidy, a takedown that did not happen is the content still being up.
            log.warn("Could not delete media object {}", objectPath.get(), e);
            return false;
        }
    }
}
