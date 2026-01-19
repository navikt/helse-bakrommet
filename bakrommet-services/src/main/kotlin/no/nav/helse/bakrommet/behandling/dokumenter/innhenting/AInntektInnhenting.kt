package no.nav.helse.bakrommet.behandling.dokumenter.innhenting

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import no.nav.helse.bakrommet.asJsonNode
import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.behandling.dokumenter.Dokument
import no.nav.helse.bakrommet.behandling.dokumenter.DokumentType
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Behandling
import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer
import no.nav.helse.bakrommet.infrastruktur.provider.AInntektFilter
import no.nav.helse.bakrommet.infrastruktur.provider.InntekterProvider
import no.nav.helse.bakrommet.infrastruktur.provider.Inntektoppslag
import no.nav.helse.bakrommet.objectMapper
import no.nav.helse.bakrommet.serialisertTilString
import no.nav.helse.yearMonth
import java.time.YearMonth

fun Dokument.somAInntektSammenlikningsgrunnlag(): Pair<Inntektoppslag, AinntektPeriodeNøkkel> {
    require(dokumentType == DokumentType.aInntekt830)
    requireNotNull(forespurteData, { "Forespurte data må være satt for å kunne tolke a-inntekt dokumentet" })
    return innhold.asJsonNode() to forespurteData.tilAinntektPeriodeNøkkel()
}

fun Dokument.somAInntektBeregningsgrunnlag(): Pair<Inntektoppslag, AinntektPeriodeNøkkel> {
    require(dokumentType == DokumentType.aInntekt828)
    requireNotNull(forespurteData, { "Forespurte data må være satt for å kunne tolke a-inntekt dokumentet" })

    return innhold.asJsonNode() to forespurteData.tilAinntektPeriodeNøkkel()
}

fun AlleDaoer.lastAInntektSammenlikningsgrunnlag(
    behandling: Behandling,
    inntekterProvider: InntekterProvider,
    saksbehandler: BrukerOgToken,
): Dokument =
    lastAInntektDok(
        periode = behandling,
        inntekterProvider = inntekterProvider,
        filter = AInntektFilter.`8-30`,
        fomMinus = 12,
        tomMinus = 1,
        saksbehandler = saksbehandler,
    )

data class AinntektPeriodeNøkkel(
    val fom: YearMonth,
    val tom: YearMonth,
)

fun String.tilAinntektPeriodeNøkkel(): AinntektPeriodeNøkkel = objectMapper.readValue(this)

fun AlleDaoer.lastAInntektBeregningsgrunnlag(
    behandling: Behandling,
    inntekterProvider: InntekterProvider,
    saksbehandler: BrukerOgToken,
): Dokument =
    lastAInntektDok(
        periode = behandling,
        inntekterProvider = inntekterProvider,
        filter = AInntektFilter.`8-28`,
        fomMinus = 3,
        tomMinus = 1,
        saksbehandler = saksbehandler,
    )

private fun doktypeFraFilter(filter: AInntektFilter): String =
    when (filter) {
        AInntektFilter.`8-28` -> DokumentType.aInntekt828
        AInntektFilter.`8-30` -> DokumentType.aInntekt830
    }

private fun AlleDaoer.lastAInntektDok(
    periode: Behandling,
    inntekterProvider: InntekterProvider,
    filter: AInntektFilter,
    fomMinus: Long,
    tomMinus: Long,
    saksbehandler: BrukerOgToken,
): Dokument {
    val skjæringstidspunkt = periode.skjæringstidspunkt

    // TODO: Bør vi ha litt slack på fom/tom?
    val fom = skjæringstidspunkt.yearMonth.minusMonths(fomMinus)
    val tom = skjæringstidspunkt.yearMonth.minusMonths(tomMinus)

    val dokType = doktypeFraFilter(filter)
    val forespurteDataNøkkel = AinntektPeriodeNøkkel(fom = fom, tom = tom).serialisertTilString()

    val alleredeLagret =
        dokumentDao.finnDokumentForForespurteData(
            behandlingId = periode.id.value,
            dokumentType = dokType,
            forespurteData = forespurteDataNøkkel,
        )
    if (alleredeLagret != null) {
        return alleredeLagret
    }

    return runBlocking {
        inntekterProvider
            .hentInntekterForMedSporing(
                fnr = periode.naturligIdent.value,
                maanedFom = fom,
                maanedTom = tom,
                filter = filter,
                saksbehandlerToken = saksbehandler.token,
            ).let { (inntekter, kildespor) ->
                // TODO: Sjekk om akkurat samme dokument med samme innhold allerede eksisterer ?
                dokumentDao.opprettDokument(
                    Dokument(
                        dokumentType = dokType,
                        eksternId = null,
                        forespurteData = forespurteDataNøkkel,
                        innhold = inntekter.serialisertTilString(),
                        sporing = kildespor,
                        opprettetForBehandling = periode.id.value,
                    ),
                )
            }
    }
}
