package com.infraView.device.rest

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
)

data class DataPacketDto(
    val readings: List<ReadingDto>
)
