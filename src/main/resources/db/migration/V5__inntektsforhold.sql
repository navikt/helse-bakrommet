CREATE TABLE IF NOT EXISTS inntektsforhold
(
    id                      UUID                        NOT NULL PRIMARY KEY,
    inntektsforhold_type    TEXT                        NOT NULL,
    sykmeldt_fra_forholdet  BOOL                        NOT NULL,
    orgnummer               TEXT                        NULL,
    orgnavn                 TEXT                        NULL,
    dagoversikt             TEXT                        NOT NULL, -- JSON
    saksbehandlingsperiode_id UUID                       NOT NULL REFERENCES saksbehandlingsperiode (id),
    opprettet               TIMESTAMP(6) WITH TIME ZONE NOT NULL
);