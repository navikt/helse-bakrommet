# E2E Tests for Helse Bakrommet

Dette er e2e-test-modulen for helse-bakrommet som inneholder komplekse integrasjonstester.

## Test-infrastruktur

### TestOppsett
- `TestApplicationOppsett.kt` - Hovedtest-infrastruktur med mock-klienter og database-oppsett
- `TestDataSource.kt` - Database-oppsett for tester
- `Http.kt` - HTTP-utilities for testing

### Mock-klienter
- `AARegMock.kt` - Mock for AAReg-klient
- `AInntektMock.kt` - Mock for A-Inntekt-klient  
- `PdlMock.kt` - Mock for PDL-klient
- `SigrunMock.kt` - Mock for Sigrun-klient
- `InntektsmeldingApiMock.kt` - Mock for Inntektsmelding-API
- `SykepengesoknadMock.kt` - Mock for Sykepengesøknad-backend
- `OAuthMock.kt` - Mock for OAuth/autentisering

## Test-typer

### E2ETest.kt
Grunnleggende end-to-end tester som verifiserer helsesjekk-endepunkter og grunnleggende funksjonalitet.

### PersonIntegrationTest.kt
Integrasjonstester for person-relatert funksjonalitet:
- Person-søk med PDL-integrasjon
- Autorisering og tilgangskontroll

### SaksbehandlingsperiodeIntegrationTest.kt
Integrasjonstester for saksbehandlingsperioder:
- Opprettelse av saksbehandlingsperioder med alle mock-klienter
- Dokumenthenting
- Utbetalingsberegning

### DatabaseIntegrationTest.kt
Tester som fokuserer på database-operasjoner:
- Lagring og henting av personer
- Komplekse database-relasjoner
- Transaksjonshåndtering

### AuthIntegrationTest.kt
Tester for autentisering og autorisering:
- Rolle-basert tilgangskontroll
- Token-validering
- OBO token exchange

## Hvordan kjøre testene

```bash
# Kjør alle e2e-tester
./gradlew :bakrommet-e2e-tests:test

# Kjør kun kompilering (for å verifisere at alt kompilerer)
./gradlew :bakrommet-e2e-tests:compileTestKotlin
```

## Status

⚠️ **Merk**: Testene er for øyeblikket satt opp med infrastruktur og eksempel-tester, men mange av API-endepunktene er ikke implementert ennå. Testene vil feile til API-endepunktene er på plass.

Testene er kommentert ut med `// assertEquals(...)` der de forventer spesifikke API-endepunkter som ikke er implementert ennå.

## Utvikling

Når du legger til nye API-endepunkter, kan du:
1. Oppdatere eksisterende tester ved å fjerne kommentarer og legge til riktige forventninger
2. Legge til nye integrasjonstester i passende test-klasser
3. Bruke `runApplicationTest { daoer -> ... }` for tilgang til database-operasjoner
4. Bruke mock-klientene for å simulere eksterne avhengigheter
