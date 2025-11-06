CREATE TABLE IF NOT EXISTS ident
(
    spillerom_id   TEXT NOT NULL PRIMARY KEY,
    naturlig_ident TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS sykepengegrunnlag
(
    id                       UUID PRIMARY KEY,
    sykepengegrunnlag        TEXT                        NULL,
    sammenlikningsgrunnlag   TEXT                        NULL,
    opprettet_for_behandling UUID                        NOT NULL,
    opprettet_av_nav_ident   TEXT                        NOT NULL,
    opprettet                TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    oppdatert                TIMESTAMP(6) WITH TIME ZONE NOT NULL
);


CREATE TABLE IF NOT EXISTS saksbehandlingsperiode
(
    id                      UUID                        NOT NULL PRIMARY KEY,
    spillerom_personid      TEXT                        NOT NULL
        REFERENCES ident (spillerom_id),
    sykepengegrunnlag_id    UUID                        NULL
        REFERENCES sykepengegrunnlag (id),
    opprettet               TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    opprettet_av_nav_ident  TEXT                        NOT NULL,
    opprettet_av_navn       TEXT                        NOT NULL,
    fom                     DATE                        NOT NULL,
    tom                     DATE                        NOT NULL,
    skjaeringstidspunkt     DATE                        NULL,
    status                  TEXT                        NOT NULL,
    beslutter_nav_ident     TEXT                        NULL,
    individuell_begrunnelse TEXT                        NULL
);

ALTER TABLE sykepengegrunnlag ADD CONSTRAINT fk_sykepengegrunnlag_behandling
    FOREIGN KEY (opprettet_for_behandling)
    REFERENCES saksbehandlingsperiode (id);


CREATE TABLE IF NOT EXISTS vurdert_vilkaar
(
    saksbehandlingsperiode_id UUID                        NOT NULL REFERENCES saksbehandlingsperiode (id),
    kode                      TEXT                        NOT NULL,
    vurdering                 TEXT                        NOT NULL,
    vurdering_tidspunkt       TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    PRIMARY KEY (saksbehandlingsperiode_id, kode)
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
    opprettet_for_behandling UUID                        NOT NULL REFERENCES saksbehandlingsperiode (id)
);

CREATE TABLE IF NOT EXISTS yrkesaktivitet
(
    id                        UUID                        NOT NULL PRIMARY KEY,
    kategorisering            TEXT                        NOT NULL,
    kategorisering_generert   TEXT                        NULL,
    dagoversikt               TEXT                        NULL,
    dagoversikt_generert      TEXT                        NULL,
    saksbehandlingsperiode_id UUID                        NOT NULL REFERENCES saksbehandlingsperiode (id),
    opprettet                 TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    generert_fra_dokumenter   TEXT                        NULL,
    perioder                  TEXT                        NULL,
    inntekt_request           TEXT                        NULL,
    inntekt_data              TEXT                        NULL,
    refusjon_data             TEXT                        NULL
);


CREATE TABLE IF NOT EXISTS saksbehandlingsperiode_endringer
(
    id                        BIGSERIAL PRIMARY KEY,
    saksbehandlingsperiode_id UUID                        NOT NULL REFERENCES saksbehandlingsperiode (id),
    status                    TEXT                        NOT NULL,
    beslutter_nav_ident       TEXT                        NULL,
    endret_tidspunkt          TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    endret_av_nav_ident       TEXT                        NOT NULL,
    endring_type              TEXT                        NOT NULL,
    endring_kommentar         TEXT                        NULL
);


CREATE TABLE IF NOT EXISTS utbetalingsberegning
(
    id                        UUID PRIMARY KEY,
    saksbehandlingsperiode_id UUID                     NOT NULL REFERENCES saksbehandlingsperiode (id) UNIQUE,
    utbetalingsberegning_data TEXT                     NOT NULL,
    opprettet                 TIMESTAMP WITH TIME ZONE NOT NULL,
    opprettet_av_nav_ident    TEXT                     NOT NULL,
    sist_oppdatert            TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS kafka_outbox
(
    id            BIGSERIAL PRIMARY KEY,
    kafka_key     TEXT                        NOT NULL,
    kafka_payload TEXT                        NOT NULL,
    opprettet     TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    publisert     TIMESTAMP(6) WITH TIME ZONE NULL
);

CREATE INDEX IF NOT EXISTS idx_kafka_outbox_unpublished
    ON kafka_outbox (publisert)
    WHERE publisert IS NULL;