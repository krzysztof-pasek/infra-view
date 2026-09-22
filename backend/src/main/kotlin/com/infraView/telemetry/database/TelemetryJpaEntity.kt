package com.infraView.telemetry.database

import com.infraView.device.database.DeviceJpaEntity
import com.infraView.incident.database.IncidentJpaEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "telemetry")
class TelemetryJpaEntity(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    var incident: IncidentJpaEntity? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    var device: DeviceJpaEntity? = null,

    @Column(name = "recorded_at", nullable = false)
    var recordedAt: OffsetDateTime,

    @Column(name = "accel_raw_x")
    var accelRawX: Double? = null,

    @Column(name = "accel_raw_y")
    var accelRawY: Double? = null,

    @Column(name = "accel_raw_z")
    var accelRawZ: Double? = null,

    @Column(name = "accel_filt_x")
    var accelFiltX: Double? = null,

    @Column(name = "accel_filt_y")
    var accelFiltY: Double? = null,

    @Column(name = "accel_filt_z")
    var accelFiltZ: Double? = null,

    @Column(name = "gyro_raw_x")
    var gyroRawX: Double? = null,

    @Column(name = "gyro_raw_y")
    var gyroRawY: Double? = null,

    @Column(name = "gyro_raw_z")
    var gyroRawZ: Double? = null,

    @Column(name = "gyro_filt_x")
    var gyroFiltX: Double? = null,

    @Column(name = "gyro_filt_y")
    var gyroFiltY: Double? = null,

    @Column(name = "gyro_filt_z")
    var gyroFiltZ: Double? = null,

    @Column(name = "temperature")
    var temperature: Double? = null,

    @Column(name = "gas_ppm")
    var gasPpm: Double? = null,

    @Column(name = "co2_ppm")
    var co2Ppm: Double? = null,

    @Column(name = "motion_state", length = 20)
    var motionState: String? = null
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}