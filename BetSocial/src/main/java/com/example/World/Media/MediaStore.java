package com.example.World.Media;

/**
 * Removing the file behind a piece of content.
 *
 * An interface with one real implementation, so the deletion path can be tested
 * without a bucket - and so that swapping Firebase Storage for anything else is
 * one class rather than every caller.
 */
public interface MediaStore {

    /**
     * Deletes the object a media URL points at. Never throws.
     *
     * Best effort on purpose. The caller is in the middle of removing a thread or
     * settling a moderation report, and a storage call that fails must not undo
     * that - a file left behind is a tidiness problem, while a failed takedown is
     * the content still being up.
     *
     * @return whether anything was actually deleted, for logging and tests
     */
    boolean delete(String media);
}
