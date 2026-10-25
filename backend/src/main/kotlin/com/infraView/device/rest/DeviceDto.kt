package com.infraView.device.rest

data class AuthRequestDto(
    val mac: String
)

data class AuthResponseDto(
    val uuid: String
)
