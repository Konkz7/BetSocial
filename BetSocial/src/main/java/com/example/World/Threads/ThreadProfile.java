package com.example.World.Threads;

import com.example.World.Users.UserView;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.data.annotation.Id;
import org.springframework.lang.NonNull;

public record ThreadProfile(
        @Id
        Long tid,
        @NonNull
        UserView user,
        @NotEmpty
        String title ,
        String media ,
        Integer media_type, // 1 is image, 2 is video
        @NotEmpty
        String category,
        @NonNull
        Long likes,
        @NonNull
        boolean liked,
        @NonNull
        Long commentCount,
        @NonNull
        Long created_at,
        @NonNull
        Boolean is_private,

        /**
         * Total staked across this thread's bets, and how many people staked it.
         *
         * The card showed "$2.5K" and "18" - written into the component, the same
         * on every thread and wrong on all of them. Zero when nobody has staked
         * yet, which is a real answer rather than a missing one.
         */
        @NonNull
        Long pool,
        @NonNull
        Long bettors
) {
}
