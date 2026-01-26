package no.nav.helse.bakrommet.e2e.organisasjon

import no.nav.helse.bakrommet.e2e.runApplicationTest
import no.nav.helse.bakrommet.e2e.testutils.ApiResult
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.hentOrganisasjonsnavn
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class OrganisasjonRouteTest {
    @Test
    fun `hent organisasjonsnavn for mocket organisasjon`() =
        runApplicationTest {
            val orgnummer = "987654321"
            val forventetNavn = "Kranførerkompaniet"

            val result = hentOrganisasjonsnavn(orgnummer)
            check(result is ApiResult.Success) { "Henting av organisasjonsnavn feilet" }
            assertEquals(forventetNavn, result.response)
        }

    @Test
    fun `hent organisasjonsnavn for annen mocket organisasjon`() =
        runApplicationTest {
            val orgnummer = "123456789"
            val forventetNavn = "Krankompisen"

            val result = hentOrganisasjonsnavn(orgnummer)
            check(result is ApiResult.Success) { "Henting av organisasjonsnavn feilet" }
            assertEquals(forventetNavn, result.response)
        }

    @Test
    fun `hent organisasjonsnavn for faker kotlin generert organisasjon`() =
        runApplicationTest {
            val orgnummer = "876547463"
            val forventetNavn = "Eide BA"

            val result = hentOrganisasjonsnavn(orgnummer)
            check(result is ApiResult.Success) { "Henting av organisasjonsnavn feilet" }
            assertEquals(forventetNavn, result.response)
        }

    @Test
    fun `organisasjon ikke funnet gir 404`() =
        runApplicationTest {
            val orgnummer = "199999999" // Ikke i mock data

            val result = hentOrganisasjonsnavn(orgnummer)
            assertIs<ApiResult.Error>(result)
            assertEquals(404, result.problemDetails.status)
        }
}
