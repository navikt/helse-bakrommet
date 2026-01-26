package no.nav.helse.bakrommet.e2e.behandling

import no.nav.helse.bakrommet.api.dto.behandling.OpprettBehandlingRequestDto
import no.nav.helse.bakrommet.domain.person.NaturligIdent
import no.nav.helse.bakrommet.e2e.runApplicationTest
import no.nav.helse.bakrommet.e2e.testutils.ApiResult
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.opprettBehandlingResult
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.personsøk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SaksbehandlingsperiodeValideringTest {
    private companion object {
        const val FNR = "01019012349"
    }

    @Test
    fun `returnerer 400 hvis fom er etter tom`() {
        runApplicationTest { _ ->
            val personPseudoId = personsøk(NaturligIdent(FNR))

            val result =
                opprettBehandlingResult(
                    personPseudoId,
                    OpprettBehandlingRequestDto(
                        fom = LocalDate.of(2023, 1, 31),
                        tom = LocalDate.of(2023, 1, 1),
                        søknader = null,
                    ),
                )

            check(result is ApiResult.Error) { "Forventer validering feil" }

            assertEquals(400, result.problemDetails.status)
            assertEquals("Fom-dato kan ikke være etter tom-dato", result.problemDetails.title)
            assertTrue(result.problemDetails.type.endsWith("validation/input"))
        }
    }
}
