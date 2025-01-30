package com.example.World.Comments;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.lang.NonNull;

public record CommentDTO(
        @NonNull
        Long tid,          // Thread ID
        Long parent_cid,    // Parent Comment ID (null for top-level comments)
        @NotEmpty
        String description
        ) {
}
