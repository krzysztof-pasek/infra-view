package com.infraView.device.domain

interface DevicePort {
    fun findByMac(mac: String): Device?
    fun findByUuid(uuid: String): Device?
    fun save(device: Device): Device
}
