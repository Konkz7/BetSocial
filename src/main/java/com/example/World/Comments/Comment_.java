package com.example.World.Comments;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.lang.NonNull;



public record Comment_(
        @Id
        Long cid,          // Unique ID for the comment
        @NonNull
        Long tid,          // Thread ID
        @NonNull
        Long uid,          // User ID
        Long parent_cid,    // Parent Comment ID (null for top-level comments)
        @NotEmpty
        String description,       // Comment text
        @NonNull
        Long created_at, // Timestamp
        Long deleted_at,
        @Version
        Integer c_version // Version number for optimistic locking
){
        @Override
        @NonNull
        public Long created_at() {
                if(deleted_at() != null){
                        if (created_at >= deleted_at){
                                throw new IllegalStateException("created_at cannot be after deleted_at");
                        }
                }

                return created_at;
        }
}
