package no.nav.helse.bakrommet.e2e.scenariotester

import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.ArbeidstakerInntektRequestDto
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.InntektRequestDto
import no.nav.helse.bakrommet.domain.etOrganisasjonsnummer
import no.nav.helse.bakrommet.e2e.testutils.AInntekt
import no.nav.helse.bakrommet.e2e.testutils.ApiResult
import no.nav.helse.bakrommet.e2e.testutils.Arbeidstaker
import no.nav.helse.bakrommet.e2e.testutils.Scenario
import no.nav.helse.bakrommet.e2e.testutils.SykAlleDager
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.hentSykepengegrunnlag
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.hentYrkesaktiviteter
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.oppdaterInntekt
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.opprettBehandlingOgForventOk
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ArbeidsgiverForlengelseTest {
    @Test
    fun `ny periode kant i kant arver sykepengegrunnlag`() {
        Scenario(
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker(etOrganisasjonsnummer(), inntekt = AInntekt(10000, 10000, 10000), dagoversikt = SykAlleDager()),
                ),
        ).runWithApplicationTestBuilder { førsteBehandling ->
            val forrigePeriode = førsteBehandling.behandling
            val personId = førsteBehandling.scenario.pseudoId
            val periode = opprettBehandlingOgForventOk(personId, forrigePeriode.tom.plusDays(1), forrigePeriode.tom.plusDays(14))
            val sykepengegrunnlag = hentSykepengegrunnlag(personId, periode.id)

            assertEquals(førsteBehandling.sykepengegrunnlag, sykepengegrunnlag!!.sykepengegrunnlag)
            assertEquals(førsteBehandling.behandling.id, sykepengegrunnlag.opprettetForBehandling)
        }
    }

    @Test
    fun `Ved arvet sykepengegrunnlag kan man ikke fastsette inntektsgrunnlag`() {
        Scenario(
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker(etOrganisasjonsnummer(), inntekt = AInntekt(10000, 10000, 10000), dagoversikt = SykAlleDager()),
                ),
        ).runWithApplicationTestBuilder { førsteBehandling ->
            val forrigePeriode = førsteBehandling.behandling
            val personId = førsteBehandling.scenario.pseudoId
            val periode = opprettBehandlingOgForventOk(personId, forrigePeriode.tom.plusDays(1), forrigePeriode.tom.plusDays(14))

            val yaer = hentYrkesaktiviteter(personId, periode.id)
            val ya = yaer.first()
            val result =
                oppdaterInntekt(
                    personId = personId,
                    behandlingId = periode.id,
                    yrkesaktivitetId = ya.id,
                    inntektRequest =
                        InntektRequestDto.Arbeidstaker(
                            data = ArbeidstakerInntektRequestDto.Ainntekt(begrunnelse = "test"),
                        ),
                )
            assertIs<ApiResult.Error>(result)
            assertEquals(400, result.problemDetails.status)
        }
    }

    @Test
    fun `ny periode med 1 dag opphold arver ikke sykepengegrunnlag`() {
        Scenario(
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker(etOrganisasjonsnummer(), inntekt = AInntekt(10000, 10000, 10000), dagoversikt = SykAlleDager()),
                ),
        ).runWithApplicationTestBuilder { førsteBehandling ->
            val forrigePeriode = førsteBehandling.behandling
            val personId = førsteBehandling.scenario.pseudoId
            val periode = opprettBehandlingOgForventOk(personId, forrigePeriode.tom.plusDays(2), forrigePeriode.tom.plusDays(14))
            val sykepengegrunnlag = hentSykepengegrunnlag(personId, periode.id)

            assertNotEquals(førsteBehandling.sykepengegrunnlag, sykepengegrunnlag?.sykepengegrunnlag)
            assertNull(sykepengegrunnlag)
        }
    }
}
