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
    tom                    DATE                        NOT NULL
);

CREATE TABLE IF NOT EXISTS vurdert_vilkaar
(
    saksbehandlingsperiode_id   UUID                        NOT NULL REFERENCES saksbehandlingsperiode (id),
    kode                        TEXT                        NOT NULL,
    vurdering                   TEXT                        NOT NULL,
    vurdering_tidspunkt         TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    PRIMARY KEY(saksbehandlingsperiode_id, kode)
);

CREATE TABLE IF NOT EXISTS dokument
(
    id                      UUID                        NOT NULL PRIMARY KEY,
    dokument_type           TEXT                        NOT NULL,   -- f.eks: "søknad" eller "a-inntekt/8-28"
    ekstern_id              TEXT                        NULL,       -- f.eks: søknads_id (ev. "N/A" ?)
    innhold                 TEXT                        NOT NULL,
    opprettet               TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    request                 TEXT                        NOT NULL, -- f.eks. get/post mot soknadsapi med parametre ditt og datt
    opprettet_for_behandling UUID                       NOT NULL REFERENCES saksbehandlingsperiode (id)
);

CREATE TABLE IF NOT EXISTS inntektsforhold
(
    id                      UUID                        NOT NULL PRIMARY KEY,
    kategorisering          TEXT                        NOT NULL,
    kategorisering_generert TEXT                        NULL,
    sykmeldt_fra_forholdet  BOOL                        NOT NULL,
    orgnummer               TEXT                        NULL,
    orgnavn                 TEXT                        NULL,
    dagoversikt             TEXT                        NULL,
    dagoversikt_generert    TEXT                        NULL,
    saksbehandlingsperiode_id UUID                      NOT NULL REFERENCES saksbehandlingsperiode (id),
    opprettet               TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    generert_fra_dokumenter TEXT                        NULL
);