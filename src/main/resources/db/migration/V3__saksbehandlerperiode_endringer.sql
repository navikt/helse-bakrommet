CREATE TABLE IF NOT EXISTS saksbehandlingsperiode_endringer
(
    id BIGSERIAL PRIMARY KEY,
    saksbehandlingsperiode_id  UUID                    NOT NULL REFERENCES saksbehandlingsperiode (id),
    -----
    status                 TEXT                        NOT NULL,
    beslutter_nav_ident    TEXT                        NULL,
    -----
    endret_tidspunkt       TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    endret_av_nav_ident    TEXT                        NOT NULL,
    endring_type           TEXT                        NOT NULL,
    endring_kommentar      TEXT                        NULL
);
