-- Opprett tabell for faktisk inntekt per inntektsforhold
CREATE TABLE faktisk_inntekt
(
    id                        UUID PRIMARY KEY,
    inntektsforhold_id        UUID NOT NULL REFERENCES inntektsforhold (id),
    belop_per_maned_ore       BIGINT NOT NULL CHECK (belop_per_maned_ore >= 0), -- Beløp i øre
    kilde                     TEXT NOT NULL CHECK (kilde IN ('AINNTEKT', 'SAKSBEHANDLER', 'SKJONNSFASTSETTELSE')),
    er_skjonnsfastsatt        BOOLEAN NOT NULL DEFAULT FALSE,
    skjonnsfastsettelse_begrunnelse TEXT,
    refusjon_belop_per_maned_ore BIGINT CHECK (refusjon_belop_per_maned_ore >= 0), -- Beløp i øre
    refusjon_grad             INTEGER CHECK (refusjon_grad >= 0 AND refusjon_grad <= 100),
    opprettet                 TIMESTAMP WITH TIME ZONE NOT NULL,
    opprettet_av_nav_ident    TEXT NOT NULL,
    
    -- Constraint: skjønnsfastsettelse krever begrunnelse
    CHECK (NOT er_skjonnsfastsatt OR skjonnsfastsettelse_begrunnelse IS NOT NULL),
    -- Constraint: refusjon må være konsistent
    CHECK ((refusjon_belop_per_maned_ore IS NULL) = (refusjon_grad IS NULL))
);

-- Opprett tabell for beregnet sykepengegrunnlag
CREATE TABLE sykepengegrunnlag
(
    id                         UUID PRIMARY KEY,
    saksbehandlingsperiode_id  UUID NOT NULL REFERENCES saksbehandlingsperiode (id),
    total_inntekt_ore          BIGINT NOT NULL CHECK (total_inntekt_ore >= 0), -- Årsinntekt i øre
    grunnbelop_6g_ore          BIGINT NOT NULL CHECK (grunnbelop_6g_ore >= 0), -- 6G i øre  
    begrenset_til_6g           BOOLEAN NOT NULL,
    sykepengegrunnlag_ore      BIGINT NOT NULL CHECK (sykepengegrunnlag_ore >= 0), -- Endelig grunnlag i øre
    begrunnelse                TEXT,
    opprettet                  TIMESTAMP WITH TIME ZONE NOT NULL,
    opprettet_av_nav_ident     TEXT NOT NULL,
    sist_oppdatert            TIMESTAMP WITH TIME ZONE NOT NULL,
    versjon                   INTEGER NOT NULL DEFAULT 1
);

-- Indekser for rask oppslag
CREATE INDEX idx_faktisk_inntekt_forhold ON faktisk_inntekt (inntektsforhold_id);
CREATE INDEX idx_sykepengegrunnlag_periode ON sykepengegrunnlag (saksbehandlingsperiode_id);

-- Kun ett aktivt sykepengegrunnlag per saksbehandlingsperiode (siste versjon)
-- Dette gjøres gjennom applikasjonens logikk, ikke database constraint
