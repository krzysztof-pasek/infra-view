package com.infraView.device.domain

import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
class DeviceService(
    private val devicePort: DevicePort
) : ManageDeviceUseCase {

    override fun registerOrFind(mac: String): Device {
        // Ten sam MAC dostaje ten sam UUID, żeby restart kasku nie rozbijał historii.
        devicePort.findByMac(mac)?.let { return it }

        val newDevice = Device(
            mac = mac,
            uuid = UUID.randomUUID().toString(),
            registeredAt = OffsetDateTime.now()
        )
        return devicePort.save(newDevice)
    }

    override fun getByUuid(uuid: String): Device? {
        return devicePort.findByUuid(uuid)
    }
}
