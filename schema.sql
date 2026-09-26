CREATE TABLE devices (
    id BIGSERIAL PRIMARY KEY,
    mac VARCHAR(17) NOT NULL UNIQUE,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    registered_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE incidents (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    firefighter_name VARCHAR(100),
    description TEXT,
    location VARCHAR(200),
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL,
    device_id BIGINT,
    CONSTRAINT fk_incidents_device FOREIGN KEY (device_id) REFERENCES devices(id)
);

CREATE UNIQUE INDEX uq_incidents_active_device ON incidents (device_id) WHERE status = 'IN_PROGRESS';

CREATE TABLE video_recordings (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ,
    file_path TEXT,
    file_size_bytes BIGINT,
    duration_sec INT,
    CONSTRAINT fk_video_recordings_incident FOREIGN KEY (incident_id) REFERENCES incidents(id)
);

CREATE TABLE telemetry (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT,
    device_id BIGINT,
    recorded_at TIMESTAMPTZ NOT NULL,
    accel_raw_x DOUBLE PRECISION,
    accel_raw_y DOUBLE PRECISION,
    accel_raw_z DOUBLE PRECISION,
    accel_filt_x DOUBLE PRECISION,
    accel_filt_y DOUBLE PRECISION,
    accel_filt_z DOUBLE PRECISION,
    gyro_raw_x DOUBLE PRECISION,
    gyro_raw_y DOUBLE PRECISION,
    gyro_raw_z DOUBLE PRECISION,
    gyro_filt_x DOUBLE PRECISION,
    gyro_filt_y DOUBLE PRECISION,
    gyro_filt_z DOUBLE PRECISION,
    temperature DOUBLE PRECISION,
    gas_ppm DOUBLE PRECISION,
    gas_voltage DOUBLE PRECISION,
    co2_ppm DOUBLE PRECISION,
    tvoc_ppb DOUBLE PRECISION,
    motion_state VARCHAR(20),
    CONSTRAINT fk_telemetry_incident FOREIGN KEY (incident_id) REFERENCES incidents(id),
    CONSTRAINT fk_telemetry_device FOREIGN KEY (device_id) REFERENCES devices(id),
    CONSTRAINT chk_telemetry_source CHECK (incident_id IS NOT NULL OR device_id IS NOT NULL)
);

CREATE INDEX idx_telemetry_incident_id ON telemetry (incident_id);
CREATE INDEX idx_telemetry_device_id_recorded_at ON telemetry (device_id, recorded_at);

CREATE TABLE alarms (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT NOT NULL,
    alarm_type VARCHAR(30) NOT NULL,
    triggered_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    CONSTRAINT fk_alarms_incident FOREIGN KEY (incident_id) REFERENCES incidents(id)
);