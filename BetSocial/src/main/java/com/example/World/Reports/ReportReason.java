package com.example.World.Reports;

/**
 * Why something was reported.
 *
 * A fixed set rather than free text: a moderator working through a queue needs
 * to sort and skim it, and the categories are also what a store review asks to
 * see. The reporter can still add detail alongside.
 */
public enum ReportReason {
    SPAM,
    ABUSE,
    HARASSMENT,
    SEXUAL_CONTENT,
    VIOLENCE,
    SELF_HARM,
    OTHER
}
