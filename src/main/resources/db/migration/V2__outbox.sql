CREATE TABLE IF NOT EXISTS kafka_outbox
(
    id                          BIGSERIAL PRIMARY KEY,
    kafka_key                   TEXT NOT NULL,
    kafka_payload               TEXT NOT NULL,
    opprettet                   TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    publisert                   TIMESTAMP(6) WITH TIME ZONE NULL
);