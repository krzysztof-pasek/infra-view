package com.infraView.device.database

import com.infraView.device.domain.Device
import com.infraView.device.domain.DevicePort
import org.springframework.stereotype.Component

@Component
class DevicePersistenceAdapter(
    private val deviceRepository: SpringDataDeviceRepository
) : DevicePort {

    override fun getByMac(mac: String): Device? {
        return deviceRepository.findByMac(mac)?.toDomain()
    }

    override fun getByUuid(uuid: String): Device? {
        return deviceRepository.findByUuid(uuid)?.toDomain()
    }

    override fun save(device: Device): Device {
        return deviceRepository.save(device.toJpaEntity()).toDomain()
    }
}

private fun Device.toJpaEntity(): DeviceJpaEntity {
    return DeviceJpaEntity(
        mac = this.mac,
        uuid = this.uuid,
        registeredAt = this.registeredAt
    ).apply {
        this.id = this@toJpaEntity.id
    }
}

private fun DeviceJpaEntity.toDomain(): Device {
    return Device(
        id = this.id,
        mac = this.mac,
        uuid = this.uuid,
        registeredAt = this.registeredAt
    )
}
