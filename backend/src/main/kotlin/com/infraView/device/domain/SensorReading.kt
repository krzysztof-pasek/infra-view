package com.infraView.device.domain

data class SensorReading(
    val sensor: String,
    val metric: String,
    val value: Double?
)
