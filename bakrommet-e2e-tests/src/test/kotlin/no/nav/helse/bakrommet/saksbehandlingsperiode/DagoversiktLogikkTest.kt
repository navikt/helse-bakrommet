package no.nav.helse.bakrommet.saksbehandlingsperiode

import no.nav.helse.bakrommet.runApplicationTest
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.TypeArbeidstaker
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.opprettSaksbehandlingsperiode
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
                opprettSaksbehandlingsperiode(
                    PERSON_ID,
                    LocalDate.parse("2023-02-01"),
                    LocalDate.parse("2023-02-28"),
                )
            // Opprett yrkesaktivitet som ordinær arbeidstaker
            val yrkesaktivitetId =
                opprettYrkesaktivitet(
                    periode.id,
                    personId = PERSON_ID,
                    YrkesaktivitetKategorisering.Arbeidstaker(
                        sykmeldt = true,
                        typeArbeidstaker = TypeArbeidstaker.Ordinær(orgnummer = "123456789"),
                    ),
                )

            // Verifiser at dagoversikt har 28 dager for februar 2023
            daoer.yrkesaktivitetDao.hentYrkesaktiviteterDbRecord(periode).also { inntektsforholdFraDB ->
                val inntektsforhold = inntektsforholdFraDB.find { it.id == yrkesaktivitetId }!!
                assertEquals(28, inntektsforhold.dagoversikt?.size ?: 0, "Februar 2023 skal ha 28 dager")
            }
        }
    }
}
