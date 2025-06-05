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
