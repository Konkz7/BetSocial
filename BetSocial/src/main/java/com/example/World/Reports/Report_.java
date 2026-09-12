package com.example.World.Reports;

import org.springframework.data.annotation.Id;
import org.springframework.lang.NonNull;

/**
 * One report, and what was done about it.
 *
 * The resolution fields are null until a moderator decides, and the database
 * enforces that they are all set together - a decision with no decider is the
 * state that makes a record worthless.
 */
public record Report_(

        @Id
        Long rid,

        @NonNull
        Long reporter_uid,

        /** THREAD, COMMENT or USER - see ReportTarget. */
        @NonNull
        String target_type,

        @NonNull
        Long target_id,

        @NonNull
        String reason,

        /** What the reporter typed, if anything. */
        String detail,

        @NonNull
        Long created_at,

        Long resolved_at,
        Long resolved_by,
        String action
) {
}
