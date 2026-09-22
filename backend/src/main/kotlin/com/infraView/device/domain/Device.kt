package com.infraView.device.domain

import java.time.OffsetDateTime

data class Device(
    val id: Long? = null,
    val mac: String,
    val uuid: String,
    val registeredAt: OffsetDateTime
)
