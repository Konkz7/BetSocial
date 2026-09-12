package com.example.World.Reports;

import jakarta.validation.constraints.NotNull;

/** A moderator's decision on a report. */
public record ReportDecisionDTO(

        @NotNull
        Long rid,

        @NotNull
        ReportAction action
) {
}
