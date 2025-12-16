package no.nav.helse.bakrommet.scenariotester

import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.YrkesaktivitetKategoriseringDto
import no.nav.helse.bakrommet.testutils.*
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.hentSykepengegrunnlag
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.hentYrkesaktiviteter
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.opprettBehandling
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
                it.scenario.pseudoId,
                it.periode.id,
            )!!.sykepengegrunnlag!!.sykepengegrunnlag `should equal` 400000.0

            hentYrkesaktiviteter(
                it.scenario.pseudoId,
                it.periode.id,
            ).first {
                when (val k = it.kategorisering) {
                    is YrkesaktivitetKategoriseringDto.Arbeidstaker -> {
                        when (val type = k.typeArbeidstaker) {
                            is no.nav.helse.bakrommet.api.dto.yrkesaktivitet.TypeArbeidstakerDto.Ordinær -> type.orgnummer == "777"
                            is no.nav.helse.bakrommet.api.dto.yrkesaktivitet.TypeArbeidstakerDto.Maritim -> type.orgnummer == "777"
                            is no.nav.helse.bakrommet.api.dto.yrkesaktivitet.TypeArbeidstakerDto.Fisker -> type.orgnummer == "777"
                            else -> false
                        }
                    }
                    is YrkesaktivitetKategoriseringDto.Frilanser -> k.orgnummer == "777"
                    else -> false
                }
            }.let { yrkesaktivitet777 ->
                slettYrkesaktivitet(
                    periodeId = it.periode.id,
                    yrkesaktivitetId = yrkesaktivitet777.id,
                    pseudoId = it.scenario.pseudoId,
                )
            }

            hentSykepengegrunnlag(
                it.scenario.pseudoId,
                it.periode.id,
            )!!.sykepengegrunnlag!!.sykepengegrunnlag `should equal` 120000.0
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
                opprettBehandling(
                    personId = it.scenario.pseudoId,
                    fom = it.periode.tom.plusDays(1),
                    tom = it.periode.tom.plusDays(14),
                )
            hentSykepengegrunnlag(
                it.scenario.pseudoId,
                nyPeriode.id,
            )!!.sykepengegrunnlag!!.sykepengegrunnlag `should equal` 400000.0

            hentYrkesaktiviteter(
                it.scenario.pseudoId,
                nyPeriode.id,
            ).also {
                it.size `should equal` 2
            }.first {
                when (val k = it.kategorisering) {
                    is YrkesaktivitetKategoriseringDto.Arbeidstaker -> {
                        when (val type = k.typeArbeidstaker) {
                            is no.nav.helse.bakrommet.api.dto.yrkesaktivitet.TypeArbeidstakerDto.Ordinær -> type.orgnummer == "777"
                            is no.nav.helse.bakrommet.api.dto.yrkesaktivitet.TypeArbeidstakerDto.Maritim -> type.orgnummer == "777"
                            is no.nav.helse.bakrommet.api.dto.yrkesaktivitet.TypeArbeidstakerDto.Fisker -> type.orgnummer == "777"
                            else -> false
                        }
                    }
                    is YrkesaktivitetKategoriseringDto.Frilanser -> k.orgnummer == "777"
                    else -> false
                }
            }.let { yrkesaktivitet777 ->
                slettYrkesaktivitet(
                    periodeId = nyPeriode.id,
                    yrkesaktivitetId = yrkesaktivitet777.id,
                    pseudoId = it.scenario.pseudoId,
                )
            }

            hentYrkesaktiviteter(
                it.scenario.pseudoId,
                nyPeriode.id,
            ).also {
                it.size `should equal` 1
            }

            hentSykepengegrunnlag(
                it.scenario.pseudoId,
                nyPeriode.id,
            )!!.sykepengegrunnlag!!.sykepengegrunnlag `should equal` 400000.0
        }
    }
}
