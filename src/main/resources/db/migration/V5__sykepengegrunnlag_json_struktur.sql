-- Drop den eksisterende faktisk_inntekt tabellen
DROP TABLE IF EXISTS faktisk_inntekt;

-- Fjern versjon kolonne fra sykepengegrunnlag
ALTER TABLE sykepengegrunnlag DROP COLUMN IF EXISTS versjon;

-- Legg til inntekter som JSON-felt
ALTER TABLE sykepengegrunnlag ADD COLUMN inntekter TEXT NOT NULL DEFAULT '[]';

-- Fjern den unødvendige indeksen
DROP INDEX IF EXISTS idx_faktisk_inntekt_forhold;

-- Kommentar: inntekter JSON struktur skal inneholde:
-- [
--   {
--     "inntektsforholdId": "uuid",
--     "beløpPerMånedØre": 5000000,
--     "kilde": "SAKSBEHANDLER",
--     "erSkjønnsfastsatt": false,
--     "skjønnsfastsettelseBegrunnelse": null,
--     "refusjon": {
--       "refusjonsbeløpPerMånedØre": 5000000,
--       "refusjonsgrad": 100
--     }
--   }
-- ]
