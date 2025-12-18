package no.nav.helse.bakrommet.behandling

import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.TypeArbeidstaker
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.person.NaturligIdent
import no.nav.helse.bakrommet.runApplicationTest
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.opprettBehandling
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.opprettYrkesaktivitet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class DagoversiktLogikkTest {
    private companion object {
        const val FNR = "01019012349"
        const val PERSON_ID = "65hth"
    }

    @Test
    fun `dagoversikt inneholder riktig antall dager for perioden`() {
        runApplicationTest { daoer ->
            daoer.personPseudoIdDao.opprettPseudoId(UUID.nameUUIDFromBytes(PERSON_ID.toByteArray()), NaturligIdent(FNR))

            // Test med februar (kortere måned) via action
            val personPseudoId = UUID.nameUUIDFromBytes(PERSON_ID.toByteArray())
            val periode =
                opprettBehandling(
                    personPseudoId,
                    LocalDate.parse("2023-02-01"),
                    LocalDate.parse("2023-02-28"),
                )
            // Opprett yrkesaktivitet som ordinær arbeidstaker
            val yrkesaktivitetId =
                opprettYrkesaktivitet(
                    personId = personPseudoId,
                    periode.id,
                    YrkesaktivitetKategorisering.Arbeidstaker(
                        sykmeldt = true,
                        typeArbeidstaker = TypeArbeidstaker.Ordinær(orgnummer = "123456789"),
                    ),
                )

            // Verifiser at dagoversikt har 28 dager for februar 2023
            daoer.yrkesaktivitetDao.hentYrkesaktiviteterDbRecord(periode.id).also { yrkesaktivitetFraDB ->
                val yrkesaktivitet = yrkesaktivitetFraDB.find { it.id == yrkesaktivitetId }!!
                assertEquals(28, yrkesaktivitet.dagoversikt?.sykdomstidlinje?.size ?: 0, "Februar 2023 skal ha 28 dager")
            }
        }
    }
}
