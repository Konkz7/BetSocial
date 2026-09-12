package com.example.World.Reports;

/**
 * A report with the thing it is about, ready to decide on.
 *
 * The content is resolved server-side and sent with the row. A moderator
 * otherwise fetches the report, then the thread or comment or user it names, and
 * then the author - three round trips per row, on a screen whose whole point is
 * getting through a list quickly. The same reasoning as PendingBetView.
 */
public record ReportView(

        Long rid,

        String target_type,
        Long target_id,

        String reason,
        String detail,

        /** The reported text itself, or the username for a report about a person. */
        String content,

        /** Who wrote it, so a moderator can see whether one account is the problem. */
        String author_name,
        Long author_uid,

        /** How many people reported this same thing. */
        long report_count,

        /** Whether the content is already gone - removed, or deleted by its author. */
        boolean already_removed,

        Long created_at
) {
}
