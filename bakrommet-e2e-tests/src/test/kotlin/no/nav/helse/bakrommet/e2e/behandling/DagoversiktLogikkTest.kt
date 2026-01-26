package no.nav.helse.bakrommet.e2e.behandling

import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.TypeArbeidstakerDto
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.YrkesaktivitetKategoriseringDto
import no.nav.helse.bakrommet.domain.enNaturligIdent
import no.nav.helse.bakrommet.domain.etOrganisasjonsnummer
import no.nav.helse.bakrommet.e2e.runApplicationTest
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.hentYrkesaktiviteter
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.opprettBehandlingOgForventOk
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.opprettYrkesaktivitetOld
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.personsøk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DagoversiktLogikkTest {
    @Test
    fun `dagoversikt inneholder riktig antall dager for perioden`() {
        runApplicationTest {
            val personPseudoId = personsøk(enNaturligIdent())

            // Test med februar (kortere måned) via action
            val behandling =
                opprettBehandlingOgForventOk(
                    personPseudoId,
                    LocalDate.parse("2023-02-01"),
                    LocalDate.parse("2023-02-28"),
                )
            // Opprett yrkesaktivitet som ordinær arbeidstaker
            val yrkesaktivitetId =
                opprettYrkesaktivitetOld(
                    personId = personPseudoId,
                    behandling.id,
                    YrkesaktivitetKategoriseringDto.Arbeidstaker(
                        sykmeldt = true,
                        typeArbeidstaker = TypeArbeidstakerDto.Ordinær(orgnummer = etOrganisasjonsnummer()),
                    ),
                )

            // Verifiser at dagoversikt har 28 dager for februar 2023
            val yrkesaktiviteter = hentYrkesaktiviteter(personPseudoId, behandling.id)
            assertEquals(1, yrkesaktiviteter.size)
            val yrkesaktivitet = yrkesaktiviteter.single()
            assertEquals(yrkesaktivitetId, yrkesaktivitet.id)
            assertEquals(28, yrkesaktivitet.dagoversikt?.size ?: 0, "Februar 2023 skal ha 28 dager")
        }
    }
}
