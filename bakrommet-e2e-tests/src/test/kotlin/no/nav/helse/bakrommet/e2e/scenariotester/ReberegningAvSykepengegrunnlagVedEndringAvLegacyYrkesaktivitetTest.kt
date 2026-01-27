package no.nav.helse.bakrommet.e2e.scenariotester

import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.TypeArbeidstakerDto
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.YrkesaktivitetKategoriseringDto
import no.nav.helse.bakrommet.domain.etOrganisasjonsnummer
import no.nav.helse.bakrommet.e2e.testutils.AInntekt
import no.nav.helse.bakrommet.e2e.testutils.Arbeidstaker
import no.nav.helse.bakrommet.e2e.testutils.Scenario
import no.nav.helse.bakrommet.e2e.testutils.SykAlleDager
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.hentSykepengegrunnlag
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.hentYrkesaktiviteter
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.opprettBehandlingOgForventOk
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.slettYrkesaktivitet
import no.nav.helse.bakrommet.e2e.testutils.`should equal`
import kotlin.test.Test

class ReberegningAvSykepengegrunnlagVedEndringAvLegacyYrkesaktivitetTest {
    @Test
    fun `sykepengegrunnlag reberegnes når vi er i første behandling`() {
        val organisasjonsnummer = etOrganisasjonsnummer()
        val etAnnetOrganisasjonsnummer = etOrganisasjonsnummer()
        Scenario(
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker(organisasjonsnummer, inntekt = AInntekt(10000, 10000, 10000), dagoversikt = SykAlleDager()),
                    Arbeidstaker(etAnnetOrganisasjonsnummer, inntekt = AInntekt(10000, 10000, 50000), dagoversikt = SykAlleDager()),
                ),
            besluttOgGodkjenn = false,
        ).runWithApplicationTestBuilder {
            hentSykepengegrunnlag(
                it.scenario.pseudoId,
                it.behandling.id,
            )!!.sykepengegrunnlag!!.sykepengegrunnlag `should equal` 400000.0

            hentYrkesaktiviteter(
                it.scenario.pseudoId,
                it.behandling.id,
            ).first {
                when (val k = it.kategorisering) {
                    is YrkesaktivitetKategoriseringDto.Arbeidstaker -> {
                        when (val type = k.typeArbeidstaker) {
                            is TypeArbeidstakerDto.Ordinær -> type.orgnummer == etAnnetOrganisasjonsnummer
                            is TypeArbeidstakerDto.Maritim -> type.orgnummer == etAnnetOrganisasjonsnummer
                            is TypeArbeidstakerDto.Fisker -> type.orgnummer == etAnnetOrganisasjonsnummer
                            else -> false
                        }
                    }

                    is YrkesaktivitetKategoriseringDto.Frilanser -> {
                        k.orgnummer == etAnnetOrganisasjonsnummer
                    }

                    else -> {
                        false
                    }
                }
            }.let { yrkesaktivitet777 ->
                slettYrkesaktivitet(
                    behandlingId = it.behandling.id,
                    yrkesaktivitetId = yrkesaktivitet777.id,
                    pseudoId = it.scenario.pseudoId,
                )
            }

            hentSykepengegrunnlag(
                it.scenario.pseudoId,
                it.behandling.id,
            )!!.sykepengegrunnlag!!.sykepengegrunnlag `should equal` 120000.0
        }
    }

    @Test
    fun `sykepengegrunnlag reberegnes ikke når vi er i forlengende behandling`() {
        val organisasjonsnummer = etOrganisasjonsnummer()
        val etAnnetOrganisasjonsnummer = etOrganisasjonsnummer()
        Scenario(
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker(organisasjonsnummer, inntekt = AInntekt(10000, 10000, 10000), dagoversikt = SykAlleDager()),
                    Arbeidstaker(etAnnetOrganisasjonsnummer, inntekt = AInntekt(10000, 10000, 50000), dagoversikt = SykAlleDager()),
                ),
            besluttOgGodkjenn = false,
        ).runWithApplicationTestBuilder {
            val nyPeriode =
                opprettBehandlingOgForventOk(
                    personId = it.scenario.pseudoId,
                    fom = it.behandling.tom.plusDays(1),
                    tom = it.behandling.tom.plusDays(14),
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
                            is TypeArbeidstakerDto.Ordinær -> type.orgnummer == etAnnetOrganisasjonsnummer
                            is TypeArbeidstakerDto.Maritim -> type.orgnummer == etAnnetOrganisasjonsnummer
                            is TypeArbeidstakerDto.Fisker -> type.orgnummer == etAnnetOrganisasjonsnummer
                            else -> false
                        }
                    }

                    is YrkesaktivitetKategoriseringDto.Frilanser -> {
                        k.orgnummer == etAnnetOrganisasjonsnummer
                    }

                    else -> {
                        false
                    }
                }
            }.let { yrkesaktivitet777 ->
                slettYrkesaktivitet(
                    behandlingId = nyPeriode.id,
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
