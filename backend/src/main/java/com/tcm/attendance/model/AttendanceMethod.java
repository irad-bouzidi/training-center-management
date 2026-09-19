package com.tcm.attendance.model;

/**
 * How the record came to exist: a trainer/admin marking a roster
 * ({@link #MANUAL}) or a student scanning the session's QR code
 * ({@link #QR}, written only by TCM-27's scan endpoint).
 */
public enum AttendanceMethod {
    MANUAL,
    QR
}
