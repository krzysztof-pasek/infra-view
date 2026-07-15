package com.infraView.telemetry

import com.infraView.incident.IncidentEntity
import jakarta.persistence.*
import java.time.OffsetDateTime

@Entity
@Table(name = "telemetry")
class TelemetryEntity(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    var incident: IncidentEntity,

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
    var temperature: Short? = null,

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