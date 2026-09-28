package com.infraView.device.domain

import java.time.OffsetDateTime

data class ThermalFrame(
    val id: Long? = null,
    val deviceId: Long,
    val incidentId: Long,
    val capturedAt: OffsetDateTime,
    val image: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ThermalFrame) return false
        return id == other.id &&
            deviceId == other.deviceId &&
            incidentId == other.incidentId &&
            capturedAt == other.capturedAt &&
            image.contentEquals(other.image)
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + deviceId.hashCode()
        result = 31 * result + incidentId.hashCode()
        result = 31 * result + capturedAt.hashCode()
        result = 31 * result + image.contentHashCode()
        return result
    }
}
