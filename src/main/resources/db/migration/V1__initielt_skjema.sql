CREATE TABLE IF NOT EXISTS ident
(
    spillerom_id   TEXT NOT NULL PRIMARY KEY,
    naturlig_ident TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS saksbehandlingsperiode
(
    id                     UUID                     NOT NULL PRIMARY KEY,
    spillerom_personid     TEXT                     NOT NULL
        REFERENCES ident (spillerom_id),
    opprettet              TIMESTAMP WITH TIME ZONE NOT NULL,
    opprettet_av_nav_ident TEXT                     NOT NULL,
    opprettet_av_navn      TEXT                     NOT NULL,
    fom                    DATE                     NOT NULL,
    tom                    DATE                     NOT NULL
);