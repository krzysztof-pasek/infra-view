package com.infraView.device.domain

interface ThermalFrameRepository {
    fun save(frame: ThermalFrame): ThermalFrame
}
