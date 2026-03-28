-- V1__create_location_tables.sql
-- TimescaleDB hypertable for GPS pings.
-- Flyway runs this against the gpsdb database (TimescaleDB instance).

-- Enable TimescaleDB extension (idempotent)
CREATE EXTENSION IF NOT EXISTS timescaledb CASCADE;

CREATE SCHEMA IF NOT EXISTS gps;

CREATE TABLE IF NOT EXISTS gps.location_pings
(
    id               UUID             NOT NULL DEFAULT gen_random_uuid(),
    courier_id       UUID             NOT NULL,
    shift_id         UUID             NOT NULL,
    latitude         DOUBLE PRECISION NOT NULL,
    longitude        DOUBLE PRECISION NOT NULL,
    accuracy_metres  DOUBLE PRECISION NOT NULL,
    speed_kmh        DOUBLE PRECISION,
    heading_degrees  DOUBLE PRECISION,
    -- recorded_at is the hypertable time dimension — MUST be NOT NULL
    recorded_at      TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    received_at      TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, recorded_at)   -- TimescaleDB requires time column in PK
);

-- Convert to hypertable partitioned by recorded_at.
-- 1-week chunks: each chunk covers 7 days of data.
-- With ~1 ping/10s per courier and 50 active couriers this is ~3M rows/week.
SELECT create_hypertable(
    'gps.location_pings',
    'recorded_at',
    chunk_time_interval => INTERVAL '7 days',
    if_not_exists => TRUE
);

-- Indexes for the common query patterns
CREATE INDEX IF NOT EXISTS idx_pings_courier_shift_time
    ON gps.location_pings (courier_id, shift_id, recorded_at DESC);

CREATE INDEX IF NOT EXISTS idx_pings_courier_time
    ON gps.location_pings (courier_id, recorded_at DESC);

-- Automatic data retention: drop chunks older than 90 days.
-- TimescaleDB drops entire chunks (files) — much faster than DELETE.
SELECT add_retention_policy(
    'gps.location_pings',
    INTERVAL '90 days',
    if_not_exists => TRUE
);

-- Continuous aggregate: 1-minute position summaries for replay UI.
-- This materialises every 1 minute of pings into a single row.
CREATE MATERIALIZED VIEW IF NOT EXISTS gps.location_pings_1min
    WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 minute', recorded_at) AS bucket,
    courier_id,
    shift_id,
    AVG(latitude)          AS avg_latitude,
    AVG(longitude)         AS avg_longitude,
    AVG(speed_kmh)         AS avg_speed_kmh,
    COUNT(*)               AS ping_count,
    MAX(recorded_at)       AS last_ping_at
FROM gps.location_pings
GROUP BY bucket, courier_id, shift_id
WITH NO DATA;

-- Refresh the continuous aggregate automatically every 1 minute
SELECT add_continuous_aggregate_policy(
    'gps.location_pings_1min',
    start_offset => INTERVAL '10 minutes',
    end_offset   => INTERVAL '1 minute',
    schedule_interval => INTERVAL '1 minute',
    if_not_exists => TRUE
);
