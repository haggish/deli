-- V1__create_route_tables.sql

CREATE TABLE IF NOT EXISTS route.shifts
(
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    courier_id     UUID         NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED',
    scheduled_date VARCHAR(10)  NOT NULL,
    started_at     TIMESTAMPTZ,
    completed_at   TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_shifts_courier_id ON route.shifts (courier_id);
CREATE INDEX IF NOT EXISTS idx_shifts_status     ON route.shifts (status);
CREATE INDEX IF NOT EXISTS idx_shifts_date       ON route.shifts (scheduled_date);

CREATE TABLE IF NOT EXISTS route.stops
(
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    shift_id              UUID         NOT NULL REFERENCES route.shifts (id) ON DELETE CASCADE,
    package_id            UUID         NOT NULL,
    customer_id           UUID         NOT NULL,
    courier_id            UUID         NOT NULL,
    sequence_number       INT          NOT NULL,
    street                VARCHAR(255) NOT NULL,
    house_number          VARCHAR(20)  NOT NULL,
    apartment             VARCHAR(50),
    floor                 INT,
    city                  VARCHAR(100) NOT NULL,
    postal_code           VARCHAR(20)  NOT NULL,
    country               VARCHAR(10)  NOT NULL,
    buzzer_code           VARCHAR(50),
    delivery_instructions TEXT,
    latitude              DOUBLE PRECISION NOT NULL,
    longitude             DOUBLE PRECISION NOT NULL,
    status                VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    delivery_status       VARCHAR(20),
    failure_reason        VARCHAR(50),
    courier_note          TEXT,
    estimated_arrival_at  TIMESTAMPTZ,
    arrived_at            TIMESTAMPTZ,
    completed_at          TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_stops_shift_id   ON route.stops (shift_id);
CREATE INDEX IF NOT EXISTS idx_stops_package_id ON route.stops (package_id);
CREATE INDEX IF NOT EXISTS idx_stops_courier_id ON route.stops (courier_id);
CREATE INDEX IF NOT EXISTS idx_stops_status     ON route.stops (status);
