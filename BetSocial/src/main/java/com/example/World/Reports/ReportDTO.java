package com.example.World.Reports;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** A report as submitted. */
public record ReportDTO(

        @NotNull
        ReportTarget target_type,

        @NotNull
        Long target_id,

        @NotNull
        ReportReason reason,

        /**
         * Optional, and capped. The column is unbounded text; a moderator reading
         * a queue is not, and neither is a push payload.
         */
        @Size(max = 1000)
        String detail
) {
}
