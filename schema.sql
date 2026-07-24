CREATE TABLE incidents (
    id SERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    firefighter_name VARCHAR(100),
    description TEXT,
    location VARCHAR(200),
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL
);

CREATE TABLE video_recordings (
    id SERIAL PRIMARY KEY,
    incident_id INT NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ,
    storage_key TEXT,
    file_size_bytes BIGINT,
    duration_sec INT,
    CONSTRAINT fk_video_recordings_incident FOREIGN KEY (incident_id) REFERENCES incidents(id)
);

CREATE TABLE telemetry (
    id BIGSERIAL PRIMARY KEY,
    incident_id INT NOT NULL,
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
    temperature SMALLINT,
    gas_ppm DOUBLE PRECISION,
    co2_ppm DOUBLE PRECISION,
    motion_state VARCHAR(20),
    CONSTRAINT fk_telemetry_incident FOREIGN KEY (incident_id) REFERENCES incidents(id)
);

CREATE TABLE alarms (
    id SERIAL PRIMARY KEY,
    incident_id INT NOT NULL,
    alarm_type VARCHAR(30) NOT NULL,
    triggered_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    CONSTRAINT fk_alarms_incident FOREIGN KEY (incident_id) REFERENCES incidents(id)
);