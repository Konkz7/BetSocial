package com.example.World.Comments;

/** Number of comments on one thread, used to count a whole feed in a single query. */
public record ThreadCommentCount(Long tid, Long comment_count) {
}
