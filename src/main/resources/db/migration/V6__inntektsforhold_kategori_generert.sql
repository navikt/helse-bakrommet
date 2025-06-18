DROP TABLE inntektsforhold;

CREATE TABLE IF NOT EXISTS inntektsforhold
(
    id                      UUID                        NOT NULL PRIMARY KEY,
    kategorisering          TEXT                        NOT NULL,
    kategorisering_generert TEXT                        NULL,
    sykmeldt_fra_forholdet  BOOL                        NOT NULL,
    orgnummer               TEXT                        NULL,
    orgnavn                 TEXT                        NULL,
    dagoversikt             TEXT                        NOT NULL,
    dagoversikt_generert    TEXT                        NULL,
    saksbehandlingsperiode_id UUID                      NOT NULL REFERENCES saksbehandlingsperiode (id),
    opprettet               TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    generert_fra_dokumenter TEXT                        NULL
);
