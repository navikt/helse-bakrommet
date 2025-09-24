CREATE INDEX IF NOT EXISTS idx_kafka_outbox_unpublished
    ON kafka_outbox (publisert)