package com.infraView.device.database

import jakarta.persistence.*
import java.time.OffsetDateTime

@Entity
@Table(name = "devices")
class DeviceJpaEntity(
    @Column(name = "mac", nullable = false, unique = true, length = 17)
    var mac: String,

    @Column(name = "uuid", nullable = false, unique = true, length = 36)
    var uuid: String,

    @Column(name = "registered_at", nullable = false)
    var registeredAt: OffsetDateTime
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}
