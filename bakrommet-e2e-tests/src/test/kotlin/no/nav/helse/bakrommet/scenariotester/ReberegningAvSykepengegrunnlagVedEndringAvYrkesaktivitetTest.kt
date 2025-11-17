package no.nav.helse.bakrommet.scenariotester

import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.domene.orgnummer
import no.nav.helse.bakrommet.testutils.*
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.hentSykepengegrunnlag
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.hentYrkesaktiviteter
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.opprettSaksbehandlingsperiode
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.slettYrkesaktivitet
import kotlin.test.Test

class ReberegningAvSykepengegrunnlagVedEndringAvYrkesaktivitetTest {
    @Test
    fun `sykepengegrunnlag reberegnes når vi er i første behandling`() {
        Scenario(
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker("888", inntekt = AInntekt(10000, 10000, 10000), dagoversikt = SykAlleDager()),
                    Arbeidstaker("777", inntekt = AInntekt(10000, 10000, 50000), dagoversikt = SykAlleDager()),
                ),
            besluttOgGodkjenn = false,
        ).runWithApplicationTestBuilder {
            hentSykepengegrunnlag(
                it.scenario.personId,
                it.periode.id,
            )!!.sykepengegrunnlag!!.sykepengegrunnlag.beløp `should equal` 400000.0

            hentYrkesaktiviteter(
                it.scenario.personId,
                it.periode.id,
            ).first {
                it.kategorisering.orgnummer() == "777"
            }.let { yrkesaktivitet777 ->
                slettYrkesaktivitet(
                    periodeId = it.periode.id,
                    yrkesaktivitetId = yrkesaktivitet777.id,
                    personId = it.scenario.personId,
                )
            }

            hentSykepengegrunnlag(
                it.scenario.personId,
                it.periode.id,
            )!!.sykepengegrunnlag!!.sykepengegrunnlag.beløp `should equal` 120000.0
        }
    }

    @Test
    fun `sykepengegrunnlag reberegnes ikke når vi er i forlengende behandling`() {
        Scenario(
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker("888", inntekt = AInntekt(10000, 10000, 10000), dagoversikt = SykAlleDager()),
                    Arbeidstaker("777", inntekt = AInntekt(10000, 10000, 50000), dagoversikt = SykAlleDager()),
                ),
            besluttOgGodkjenn = false,
        ).runWithApplicationTestBuilder {
            val nyPeriode =
                opprettSaksbehandlingsperiode(
                    personId = it.scenario.personId,
                    fom = it.periode.tom.plusDays(1),
                    tom = it.periode.tom.plusDays(14),
                )
            hentSykepengegrunnlag(
                it.scenario.personId,
                nyPeriode.id,
            )!!.sykepengegrunnlag!!.sykepengegrunnlag.beløp `should equal` 400000.0

            hentYrkesaktiviteter(
                it.scenario.personId,
                nyPeriode.id,
            ).also {
                it.size `should equal` 2
            }.first {
                it.kategorisering.orgnummer() == "777"
            }.let { yrkesaktivitet777 ->
                slettYrkesaktivitet(
                    periodeId = nyPeriode.id,
                    yrkesaktivitetId = yrkesaktivitet777.id,
                    personId = it.scenario.personId,
                )
            }

            hentYrkesaktiviteter(
                it.scenario.personId,
                nyPeriode.id,
            ).also {
                it.size `should equal` 1
            }

            hentSykepengegrunnlag(
                it.scenario.personId,
                nyPeriode.id,
            )!!.sykepengegrunnlag!!.sykepengegrunnlag.beløp `should equal` 400000.0
        }
    }
}
