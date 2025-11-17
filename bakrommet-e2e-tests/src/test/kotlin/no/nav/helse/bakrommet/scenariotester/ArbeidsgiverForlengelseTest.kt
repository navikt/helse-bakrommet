package no.nav.helse.bakrommet.scenariotester

import io.ktor.http.*
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.ArbeidstakerInntektRequest
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.InntektRequest
import no.nav.helse.bakrommet.testutils.AInntekt
import no.nav.helse.bakrommet.testutils.Arbeidstaker
import no.nav.helse.bakrommet.testutils.Scenario
import no.nav.helse.bakrommet.testutils.SykAlleDager
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.hentSykepengegrunnlag
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.hentYrkesaktiviteter
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.oppdaterInntekt
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.opprettSaksbehandlingsperiode
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import kotlin.test.assertEquals

class ArbeidsgiverForlengelseTest {
    @Test
    fun `ny periode kant i kant arver sykepengegrunnlag`() {
        Scenario(
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker("888", inntekt = AInntekt(10000, 10000, 10000), dagoversikt = SykAlleDager()),
                ),
        ).runWithApplicationTestBuilder { førsteBehandling ->
            val forrigePeriode = førsteBehandling.periode
            val personId = førsteBehandling.scenario.personId
            val periode = opprettSaksbehandlingsperiode(personId, forrigePeriode.tom.plusDays(1), forrigePeriode.tom.plusDays(14))
            val sykepengegrunnlag = hentSykepengegrunnlag(periode.spilleromPersonId, periode.id)

            assertEquals(førsteBehandling.sykepengegrunnlag, sykepengegrunnlag!!.sykepengegrunnlag)
            assertEquals(førsteBehandling.periode.id, sykepengegrunnlag.opprettetForBehandling)
        }
    }

    @Test
    fun `Ved arvet sykepengegrunnlag kan man ikke fastsette inntektsgrunnlag`() {
        Scenario(
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker("888", inntekt = AInntekt(10000, 10000, 10000), dagoversikt = SykAlleDager()),
                ),
        ).runWithApplicationTestBuilder { førsteBehandling ->
            val forrigePeriode = førsteBehandling.periode
            val personId = førsteBehandling.scenario.personId
            val periode = opprettSaksbehandlingsperiode(personId, forrigePeriode.tom.plusDays(1), forrigePeriode.tom.plusDays(14))

            val yaer = hentYrkesaktiviteter(personId, periode.id)
            val ya = yaer.first()
            oppdaterInntekt(
                personId = personId,
                periodeId = periode.id,
                yrkesaktivitetId = ya.id,
                inntektRequest =
                    InntektRequest.Arbeidstaker(
                        data = ArbeidstakerInntektRequest.Ainntekt(begrunnelse = "test"),
                    ),
                expectedResponseStatus = HttpStatusCode.BadRequest,
            )
        }
    }

    @Test
    fun `ny periode med 1 dag opphold arver ikke sykepengegrunnlag`() {
        Scenario(
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker("888", inntekt = AInntekt(10000, 10000, 10000), dagoversikt = SykAlleDager()),
                ),
        ).runWithApplicationTestBuilder { førsteBehandling ->
            val forrigePeriode = førsteBehandling.periode
            val personId = førsteBehandling.scenario.personId
            val periode = opprettSaksbehandlingsperiode(personId, forrigePeriode.tom.plusDays(2), forrigePeriode.tom.plusDays(14))
            val sykepengegrunnlag = hentSykepengegrunnlag(periode.spilleromPersonId, periode.id)

            assertNotEquals(førsteBehandling.sykepengegrunnlag, sykepengegrunnlag?.sykepengegrunnlag)
            assertNull(sykepengegrunnlag)
        }
    }
}
