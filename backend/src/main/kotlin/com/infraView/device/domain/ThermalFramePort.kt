package com.infraView.device.domain

interface ThermalFramePort {
    fun save(frame: ThermalFrame): ThermalFrame
}
