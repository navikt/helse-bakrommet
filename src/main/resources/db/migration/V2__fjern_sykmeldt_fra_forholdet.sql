-- Flyway migration: fjern kolonnen sykmeldt_fra_forholdet fra inntektsforhold
ALTER TABLE inntektsforhold
    DROP COLUMN IF EXISTS sykmeldt_fra_forholdet; 