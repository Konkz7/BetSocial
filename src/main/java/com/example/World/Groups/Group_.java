package com.example.World.Groups;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.lang.NonNull;



public record Group_(
        @Id
        Long gid, // Primary key
        @NotEmpty
        String group_name,
        @NonNull
        Integer sort,  //  representing the sort of conversation (direct or group)
        @NonNull
        Long created_at, // Timestamp of conversation creation
        Long deleted_at,
        @Version
        Integer g_version // Version number for optimistic locking
) {
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
