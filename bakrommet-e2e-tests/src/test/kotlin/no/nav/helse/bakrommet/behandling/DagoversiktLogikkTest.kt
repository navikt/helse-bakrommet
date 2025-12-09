package no.nav.helse.bakrommet.behandling

import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.TypeArbeidstaker
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.runApplicationTest
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.opprettBehandling
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.opprettYrkesaktivitet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DagoversiktLogikkTest {
    private companion object {
        const val FNR = "01019012349"
        const val PERSON_ID = "65hth"
    }

    @Test
    fun `dagoversikt inneholder riktig antall dager for perioden`() {
        runApplicationTest { daoer ->
            daoer.personDao.opprettPerson(FNR, PERSON_ID)

            // Test med februar (kortere måned) via action
            val periode =
                opprettBehandling(
                    PERSON_ID,
                    LocalDate.parse("2023-02-01"),
                    LocalDate.parse("2023-02-28"),
                )
            // Opprett yrkesaktivitet som ordinær arbeidstaker
            val yrkesaktivitetId =
                opprettYrkesaktivitet(
                    personId = PERSON_ID,
                    periode.id,
                    YrkesaktivitetKategorisering.Arbeidstaker(
                        sykmeldt = true,
                        typeArbeidstaker = TypeArbeidstaker.Ordinær(orgnummer = "123456789"),
                    ),
                )

            // Verifiser at dagoversikt har 28 dager for februar 2023
            daoer.yrkesaktivitetDao.hentYrkesaktiviteterDbRecord(periode).also { yrkesaktivitetFraDB ->
                val yrkesaktivitet = yrkesaktivitetFraDB.find { it.id == yrkesaktivitetId }!!
                assertEquals(28, yrkesaktivitet.dagoversikt?.size ?: 0, "Februar 2023 skal ha 28 dager")
            }
        }
    }
}
