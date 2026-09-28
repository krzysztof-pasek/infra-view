package com.infraView.device.domain

interface DeviceRepository {
    fun getByMac(mac: String): Device?
    fun getByUuid(uuid: String): Device?
    fun save(device: Device): Device
}
