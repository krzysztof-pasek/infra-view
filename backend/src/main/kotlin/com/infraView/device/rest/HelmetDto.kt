package com.infraView.device.rest

import com.infraView.device.domain.SensorReading

data class AuthRequestDto(
    val mac: String
)

data class AuthResponseDto(
    val uuid: String
)

data class ReadingDto(
    val sensor: String,
    val metric: String,
    val value: Double?,
    val unit: String
) {
    fun toDomain() = SensorReading(
        sensor = this.sensor,
        metric = this.metric,
        value = this.value
    )
}

data class DataPacketDto(
    val readings: List<ReadingDto>
)
