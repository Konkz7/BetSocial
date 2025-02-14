package com.example.World.Threads;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.lang.NonNull;





public record Thread_(
        @Id
        Long tid,
        @NonNull
        Long uid,
        @NotEmpty
        String title ,
        String description ,
        @NotEmpty
        String category,
        @NonNull
        Long created_at,
        Long deleted_at,
        @NonNull
        Boolean is_private,
        @Version
        Integer t_version
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
