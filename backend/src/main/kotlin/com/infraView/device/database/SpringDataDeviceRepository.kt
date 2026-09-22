package com.infraView.device.database

import org.springframework.data.jpa.repository.JpaRepository

interface SpringDataDeviceRepository : JpaRepository<DeviceJpaEntity, Long> {
    fun findByMac(mac: String): DeviceJpaEntity?
    fun findByUuid(uuid: String): DeviceJpaEntity?
}
