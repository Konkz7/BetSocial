package com.example.World.Threads;

import java.util.List;

/**
 * One page of the feed, and how to ask for the next one.
 *
 * The cursor is sent back rather than left to the client to work out. A client
 * that builds its own cursor from the last row it rendered gets it wrong the
 * moment the shape of the ordering changes, and it cannot know the difference
 * between "this page was short because the feed ended" and "short because of
 * filtering" - so the server says outright whether there is more.
 */
public record FeedPage(

        List<ThreadProfile> threads,

        /**
         * Pass back as cursor_created_at and cursor_tid to get the next page.
         * Null when there is nothing after this.
         */
        Long next_cursor_created_at,
        Long next_cursor_tid,

        boolean has_more
) {
}
