CREATE TABLE IF NOT EXISTS ident
(
    spillerom_id   TEXT NOT NULL PRIMARY KEY,
    naturlig_ident TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS saksbehandlingsperiode
(
    id                     UUID                        NOT NULL PRIMARY KEY,
    spillerom_personid     TEXT                        NOT NULL
        REFERENCES ident (spillerom_id),
    opprettet              TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    opprettet_av_nav_ident TEXT                        NOT NULL,
    opprettet_av_navn      TEXT                        NOT NULL,
    fom                    DATE                        NOT NULL,
    tom                    DATE                        NOT NULL,
    skjaeringstidspunkt    DATE                        NULL,
    status                 TEXT                        NOT NULL,
    beslutter_nav_ident    TEXT                        NULL
);

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
    request                  TEXT                        NOT NULL,
    opprettet_for_behandling UUID                        NOT NULL REFERENCES saksbehandlingsperiode (id)
);

CREATE TABLE IF NOT EXISTS yrkesaktivitet
(
    id                        UUID                        NOT NULL PRIMARY KEY,
    kategorisering            TEXT                        NOT NULL,
    kategorisering_generert   TEXT                        NULL,
    orgnummer                 TEXT                        NULL,
    dagoversikt               TEXT                        NULL,
    dagoversikt_generert      TEXT                        NULL,
    saksbehandlingsperiode_id UUID                        NOT NULL REFERENCES saksbehandlingsperiode (id),
    opprettet                 TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    generert_fra_dokumenter   TEXT                        NULL
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

CREATE TABLE IF NOT EXISTS sykepengegrunnlag
(
    id                            UUID PRIMARY KEY,
    saksbehandlingsperiode_id     UUID                     NOT NULL REFERENCES saksbehandlingsperiode (id) UNIQUE,
    total_inntekt_ore             INTEGER                  NOT NULL,
    grunnbelop_ore                INTEGER                  NOT NULL,
    grunnbelop_6g_ore             INTEGER                  NOT NULL,
    begrenset_til_6g              BOOLEAN                  NOT NULL,
    sykepengegrunnlag_ore         INTEGER                  NOT NULL,
    begrunnelse                   TEXT,
    opprettet                     TIMESTAMP WITH TIME ZONE NOT NULL,
    opprettet_av_nav_ident        TEXT                     NOT NULL,
    sist_oppdatert                TIMESTAMP WITH TIME ZONE NOT NULL,
    inntekter                     TEXT                     NOT NULL,
    grunnbelop_virkningstidspunkt DATE                     NOT NULL
);

CREATE TABLE IF NOT EXISTS utbetalingsberegning
(
    id                            UUID PRIMARY KEY,
    saksbehandlingsperiode_id     UUID                     NOT NULL REFERENCES saksbehandlingsperiode (id) UNIQUE,
    utbetalingsberegning_data                TEXT                     NOT NULL,
    opprettet                     TIMESTAMP WITH TIME ZONE NOT NULL,
    opprettet_av_nav_ident        TEXT                     NOT NULL,
    sist_oppdatert                TIMESTAMP WITH TIME ZONE NOT NULL
);
