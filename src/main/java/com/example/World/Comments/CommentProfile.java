package com.example.World.Comments;

import java.util.List;

public class CommentProfile {

        public Long cid;              // Unique ID for the comment
        public Long tid;              // Thread ID
        public Long uid;              // User ID
        public List<CommentProfile> replies; // Nested replies
        public String user_name;
        public String profile_picture;
        public Long parent_cid;       // Parent Comment ID (null for top-level comments)
        public String description;    // Comment text
        public Long likes;
        public boolean liked;
        public Long created_at;       // Timestamp

        // All-args constructor
        public CommentProfile(Long cid, Long tid, Long uid, List<CommentProfile> replies,
                              String user_name, String profile_picture, Long parent_cid,
                              String description, Long likes , boolean liked, Long created_at) {
                this.cid = cid;
                this.tid = tid;
                this.uid = uid;
                this.replies = replies;
                this.user_name = user_name;
                this.profile_picture = profile_picture;
                this.parent_cid = parent_cid;
                this.description = description;
                this.created_at = created_at;
                this.likes = likes;
                this.liked = liked;
        }
}
