package com.infraView.telemetry.domain

enum class MotionState {
    /** Helmet has not moved yet in this segment (e.g. lying on a table before it is put on). */
    IDLE,
    MOVING,
    STILL,
    /** Posture changed a lot after moving and the firefighter has been still since. */
    FALLEN,
    /** Still for a long time with no posture change (PASS-style man-down). */
    NO_MOTION
}
