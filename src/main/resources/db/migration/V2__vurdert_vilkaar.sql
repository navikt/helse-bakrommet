CREATE TABLE IF NOT EXISTS vurdert_vilkaar
(
    --id                          UUID                        NOT NULL PRIMARY KEY,
    saksbehandlingsperiode_id   UUID                        NOT NULL REFERENCES saksbehandlingsperiode (id),
    kode                        TEXT                        NOT NULL,
    vurdering                   JSON                        NOT NULL,
    vurdering_tidspunkt         TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    --saksbehandler_ident         TEXT
    --status                      TEXT                        NOT NULL,
    --begrunnelse_kode            TEXT                        NOT NULL,
    --begrunnelse_tekst           TEXT                        NOT NULL,

        --gjeldende               BOOL                        NOT NULL DEFAULT TRUE,
    PRIMARY KEY(saksbehandlingsperiode_id, kode)
);

--CREATE UNIQUE INDEX "vurdert_vilkaar_gjeldende" ON vurdert_vilkaar(saksbehandlingsperiode_id,kode) WHERE gjeldende=true;
--CREATE UNIQUE INDEX "vurdert_vilkaar_per_behandling" ON vurdert_vilkaar(saksbehandlingsperiode_id,kode)

-- NB: Må addes transaksjonelt med UPDATE på vurdert_vilkaar
-- CREATE TABLE IF NOT EXISTS vurdert_vilkaar_hist
-- (
--     id                          UUID                        NOT NULL,
--     erstattet                   TIMESTAMP(6) WITH TIME ZONE NOT NULL,
--     data                        TEXT                        NOT NULL
-- );
