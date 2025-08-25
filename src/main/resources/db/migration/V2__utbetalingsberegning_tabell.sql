CREATE TABLE IF NOT EXISTS utbetalingsberegning
(
    id                            UUID PRIMARY KEY,
    saksbehandlingsperiode_id     UUID                     NOT NULL REFERENCES saksbehandlingsperiode (id) UNIQUE,
    utbetalingsberegning_data                TEXT                     NOT NULL,
    opprettet                     TIMESTAMP WITH TIME ZONE NOT NULL,
    opprettet_av_nav_ident        TEXT                     NOT NULL,
    sist_oppdatert                TIMESTAMP WITH TIME ZONE NOT NULL
);
