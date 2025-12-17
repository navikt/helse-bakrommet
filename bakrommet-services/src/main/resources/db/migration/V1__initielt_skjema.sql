CREATE TABLE IF NOT EXISTS person_pseudo_id
(
    pseudo_id      UUID                        NOT NULL PRIMARY KEY,
    naturlig_ident VARCHAR(11)                 NOT NULL,
    opprettet      TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_personpseudoid_naturlig_ident
    ON person_pseudo_id (naturlig_ident);


CREATE TABLE IF NOT EXISTS sykepengegrunnlag
(
    id                       UUID PRIMARY KEY,
    sykepengegrunnlag        JSONB                        NULL,
    sammenlikningsgrunnlag   JSONB                        NULL,
    opprettet_for_behandling UUID                        NOT NULL,
    opprettet_av_nav_ident   TEXT                        NOT NULL,
    opprettet                TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    oppdatert                TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    laast                    BOOLEAN                     NOT NULL
);


CREATE TABLE IF NOT EXISTS behandling
(
    id                         UUID                        NOT NULL PRIMARY KEY,
    naturlig_ident             VARCHAR(11)                 NOT NULL,
    sykepengegrunnlag_id       UUID                        NULL
        REFERENCES sykepengegrunnlag (id),
    opprettet                  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    opprettet_av_nav_ident     TEXT                        NOT NULL,
    opprettet_av_navn          TEXT                        NOT NULL,
    fom                        DATE                        NOT NULL,
    tom                        DATE                        NOT NULL,
    skjaeringstidspunkt        DATE                        NOT NULL,
    status                     TEXT                        NOT NULL,
    beslutter_nav_ident        TEXT                        NULL,
    individuell_begrunnelse    TEXT                        NULL,
    revurderer_behandling_id   UUID                        NULL UNIQUE REFERENCES behandling (id),
    revurdert_av_behandling_id UUID                        NULL UNIQUE REFERENCES behandling (id)
);

ALTER TABLE sykepengegrunnlag
    ADD CONSTRAINT fk_sykepengegrunnlag_behandling
        FOREIGN KEY (opprettet_for_behandling)
            REFERENCES behandling (id);


CREATE TABLE IF NOT EXISTS vurdert_vilkaar
(
    behandling_id       UUID                        NOT NULL REFERENCES behandling (id),
    kode                TEXT                        NOT NULL,
    vurdering           JSONB                        NOT NULL,
    vurdering_tidspunkt TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    PRIMARY KEY (behandling_id, kode)
);

CREATE TABLE IF NOT EXISTS dokument
(
    id                       UUID                        NOT NULL PRIMARY KEY,
    dokument_type            TEXT                        NOT NULL,
    ekstern_id               TEXT                        NULL,
    innhold                  TEXT                        NOT NULL,
    opprettet                TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    sporing                  TEXT                        NOT NULL,
    forespurte_data          TEXT                        NULL,
    opprettet_for_behandling UUID                        NOT NULL REFERENCES behandling (id)
);

CREATE TABLE IF NOT EXISTS yrkesaktivitet
(
    id                      UUID                        NOT NULL PRIMARY KEY,
    behandling_id           UUID                        NOT NULL REFERENCES behandling (id),
    kategorisering          JSONB                        NOT NULL,
    kategorisering_generert JSONB                        NULL,
    dagoversikt             JSONB                        NULL,
    dagoversikt_generert    JSONB                        NULL,
    opprettet               TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    generert_fra_dokumenter JSONB                        NULL,
    perioder                JSONB                        NULL,
    inntekt_request         JSONB                        NULL,
    inntekt_data            JSONB                        NULL,
    refusjon                JSONB                        NULL
);


CREATE TABLE IF NOT EXISTS behandling_endringer
(
    id                  BIGSERIAL PRIMARY KEY,
    behandling_id       UUID                        NOT NULL REFERENCES behandling (id),
    status              TEXT                        NOT NULL,
    beslutter_nav_ident TEXT                        NULL,
    endret_tidspunkt    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    endret_av_nav_ident TEXT                        NOT NULL,
    endring_type        TEXT                        NOT NULL,
    endring_kommentar   TEXT                        NULL
);


CREATE TABLE IF NOT EXISTS utbetalingsberegning
(
    id                        UUID PRIMARY KEY,
    behandling_id             UUID                     NOT NULL REFERENCES behandling (id) UNIQUE,
    utbetalingsberegning_data JSONB                    NOT NULL,
    opprettet                 TIMESTAMP WITH TIME ZONE NOT NULL,
    opprettet_av_nav_ident    TEXT                     NOT NULL,
    sist_oppdatert            TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS kafka_outbox
(
    id        BIGSERIAL PRIMARY KEY,
    key       TEXT                        NOT NULL,
    payload   JSON                        NOT NULL,
    topic     TEXT                        NOT NULL,
    opprettet TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    publisert TIMESTAMP(6) WITH TIME ZONE NULL
);

CREATE INDEX IF NOT EXISTS idx_kafka_outbox_unpublished
    ON kafka_outbox (publisert)
    WHERE publisert IS NULL;

CREATE TABLE IF NOT EXISTS tilkommen_inntekt
(
    id                     UUID                        NOT NULL PRIMARY KEY,
    behandling_id          UUID                        NOT NULL REFERENCES behandling (id),
    tilkommen_inntekt      JSONB                        NOT NULL,
    opprettet              TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    opprettet_av_nav_ident TEXT                        NOT NULL
);
