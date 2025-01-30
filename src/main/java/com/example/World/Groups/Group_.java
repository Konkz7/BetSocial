package com.example.World.Groups;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.lang.NonNull;

import java.time.LocalDateTime;

public record Group_(
        @Id
        Long gid, // Primary key
        @NotEmpty
        String group_name,
        @NonNull
        Integer sort,  //  representing the sort of conversation (direct or group)
        @NonNull
        LocalDateTime created_at, // Timestamp of conversation creation
        LocalDateTime deleted_at,
        @Version
        Integer g_version // Version number for optimistic locking
) {
        @Override
        @NonNull
        public LocalDateTime created_at() {
                if(deleted_at() != null){
                        if (created_at.isAfter(deleted_at())){
                                throw new IllegalStateException("created_at cannot be after deleted_at");
                        }
                }

                return created_at;
        }
}
