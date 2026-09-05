package com.tcm.user.model;

/**
 * The three application roles, per docs/PLAN.md §5. Stored as a varchar +
 * check constraint on the {@code users} table rather than a Postgres enum
 * type, so adding a value later is a one-line CHECK change.
 */
public enum Role {
    ADMIN,
    TRAINER,
    STUDENT
}
