package no.nav.helse.bakrommet.e2e.behandling

import no.nav.helse.bakrommet.domain.enNaturligIdent
import no.nav.helse.bakrommet.e2e.runApplicationTest
import no.nav.helse.bakrommet.e2e.testutils.ApiResult
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.opprettBehandling
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.personsøk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertIs

class SaksbehandlingsperiodeValideringTest {
    private val naturligIdent = enNaturligIdent()

    @Test
    fun `returnerer 400 hvis fom er etter tom`() {
        runApplicationTest {
            val personPseudoId = personsøk(naturligIdent)

            val result =
                opprettBehandling(
                    personPseudoId,
                    fom = LocalDate.of(2023, 1, 31),
                    tom = LocalDate.of(2023, 1, 1),
                    søknader = null,
                )

            assertIs<ApiResult.Error>(result)
            assertEquals(400, result.problemDetails.status)
            assertEquals("Fom-dato kan ikke være etter tom-dato", result.problemDetails.title)
            assertTrue(result.problemDetails.type.endsWith("validation/input"))
        }
    }
}
