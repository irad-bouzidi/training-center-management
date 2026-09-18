package com.tcm.schedule.model;

/**
 * Class session lifecycle, per docs/PLAN.md §5. {@code SCHEDULED} on
 * creation; an ADMIN may {@code CANCELLED} it, and an ADMIN or the assigned
 * trainer marks it {@code COMPLETED} once it has been delivered. There is no
 * way back to {@code SCHEDULED} - a cancelled session is re-created rather
 * than reopened.
 */
public enum SessionStatus {
    SCHEDULED,
    CANCELLED,
    COMPLETED
}
