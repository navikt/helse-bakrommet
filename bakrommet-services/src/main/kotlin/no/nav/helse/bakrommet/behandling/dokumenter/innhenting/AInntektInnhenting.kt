package no.nav.helse.bakrommet.behandling.dokumenter.innhenting

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import no.nav.helse.bakrommet.ainntekt.AInntektClient
import no.nav.helse.bakrommet.ainntekt.AInntektFilter
import no.nav.helse.bakrommet.ainntekt.Inntektoppslag
import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.behandling.Behandling
import no.nav.helse.bakrommet.behandling.dokumenter.Dokument
import no.nav.helse.bakrommet.behandling.dokumenter.DokumentType
import no.nav.helse.bakrommet.util.asJsonNode
import no.nav.helse.bakrommet.util.objectMapper
import no.nav.helse.bakrommet.util.serialisertTilString
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

fun DokumentInnhentingDaoer.lastAInntektSammenlikningsgrunnlag(
    periode: Behandling,
    aInntektClient: AInntektClient,
    saksbehandler: BrukerOgToken,
): Dokument =
    lastAInntektDok(
        periode = periode,
        aInntektClient = aInntektClient,
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

fun DokumentInnhentingDaoer.lastAInntektBeregningsgrunnlag(
    periode: Behandling,
    aInntektClient: AInntektClient,
    saksbehandler: BrukerOgToken,
): Dokument =
    lastAInntektDok(
        periode = periode,
        aInntektClient = aInntektClient,
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

private fun DokumentInnhentingDaoer.lastAInntektDok(
    periode: Behandling,
    aInntektClient: AInntektClient,
    filter: AInntektFilter,
    fomMinus: Long,
    tomMinus: Long,
    saksbehandler: BrukerOgToken,
): Dokument {
    val skjæringstidspunkt =
        periode.skjæringstidspunkt ?: throw IllegalStateException("Skjæringstidspunkt må være satt for å hente inntekt")

    // TODO: Bør vi ha litt slack på fom/tom?
    val fom = skjæringstidspunkt.yearMonth.minusMonths(fomMinus)
    val tom = skjæringstidspunkt.yearMonth.minusMonths(tomMinus)

    val dokType = doktypeFraFilter(filter)
    val forespurteDataNøkkel = AinntektPeriodeNøkkel(fom = fom, tom = tom).serialisertTilString()

    val alleredeLagret =
        dokumentDao.finnDokumentForForespurteData(
            behandlingId = periode.id,
            dokumentType = dokType,
            forespurteData = forespurteDataNøkkel,
        )
    if (alleredeLagret != null) {
        return alleredeLagret
    }

    return runBlocking {
        aInntektClient
            .hentInntekterForMedSporing(
                fnr = periode.naturligIdent.naturligIdent,
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
                        opprettetForBehandling = periode.id,
                    ),
                )
            }
    }
}
