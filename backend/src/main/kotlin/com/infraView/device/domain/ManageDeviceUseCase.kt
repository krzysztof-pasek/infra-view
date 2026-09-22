package com.infraView.device.domain

interface ManageDeviceUseCase {
    fun registerOrFind(mac: String): Device
    fun getByUuid(uuid: String): Device?
}
