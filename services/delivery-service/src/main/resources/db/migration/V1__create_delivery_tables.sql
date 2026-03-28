-- V1__create_delivery_tables.sql

CREATE TABLE IF NOT EXISTS delivery.delivery_records
(
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    stop_id          UUID        NOT NULL UNIQUE,
    shift_id         UUID        NOT NULL,
    package_id       UUID        NOT NULL,
    customer_id      UUID        NOT NULL,
    courier_id       UUID        NOT NULL,
    tracking_number  VARCHAR(50) NOT NULL DEFAULT '',
    status           VARCHAR(20) NOT NULL DEFAULT 'ASSIGNED',
    placement        VARCHAR(50),
    failure_reason   VARCHAR(50),
    courier_note     TEXT,
    proof_photo_key  VARCHAR(500),
    signature_key    VARCHAR(500),
    confirmed_at     TIMESTAMPTZ,
    attempt_number   INT         NOT NULL DEFAULT 1,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_delivery_stop_id     ON delivery.delivery_records (stop_id);
CREATE INDEX IF NOT EXISTS idx_delivery_package_id  ON delivery.delivery_records (package_id);
CREATE INDEX IF NOT EXISTS idx_delivery_courier_id  ON delivery.delivery_records (courier_id);
CREATE INDEX IF NOT EXISTS idx_delivery_shift_id    ON delivery.delivery_records (shift_id);
CREATE INDEX IF NOT EXISTS idx_delivery_status      ON delivery.delivery_records (status);
