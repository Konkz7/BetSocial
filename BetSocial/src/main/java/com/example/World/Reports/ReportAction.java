package com.example.World.Reports;

/** What a moderator did about a report. */
public enum ReportAction {
    /** The content was taken down. */
    REMOVED,
    /** The account was suspended, for a report about a person. */
    SUSPENDED,
    /** Looked at, nothing wrong with it. */
    DISMISSED
}
