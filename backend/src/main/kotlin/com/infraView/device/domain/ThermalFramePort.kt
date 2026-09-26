package com.infraView.device.domain

interface ThermalFramePort {
    fun saveLatest(deviceId: Long, jpeg: ByteArray)
}
