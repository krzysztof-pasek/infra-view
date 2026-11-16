package com.infraView.device.domain

import java.time.OffsetDateTime

class LiveFrame(
    val sequence: Long,
    val incidentId: Long,
    val capturedAt: OffsetDateTime,
    val image: ByteArray
)
