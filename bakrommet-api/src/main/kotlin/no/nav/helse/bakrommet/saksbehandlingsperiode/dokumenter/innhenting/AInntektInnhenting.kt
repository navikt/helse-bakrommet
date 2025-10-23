package no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.innhenting

import kotlinx.coroutines.runBlocking
import no.nav.helse.bakrommet.ainntekt.AInntektClient
import no.nav.helse.bakrommet.ainntekt.AInntektFilter
import no.nav.helse.bakrommet.ainntekt.Inntektoppslag
import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.Dokument
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.DokumentType
import no.nav.helse.bakrommet.util.asJsonNode
import no.nav.helse.bakrommet.util.serialisertTilString
import no.nav.helse.yearMonth

fun Dokument.somAInntektSammenlikningsgrunnlag(): Inntektoppslag {
    require(dokumentType == DokumentType.aInntekt830)
    return innhold.asJsonNode()
}

fun Dokument.somAInntektBeregningsgrunnlag(): Inntektoppslag {
    require(dokumentType == DokumentType.aInntekt828)
    return innhold.asJsonNode()
}

fun DokumentInnhentingDaoer.lastAInntektSammenlikningsgrunnlag(
    periode: Saksbehandlingsperiode,
    aInntektClient: AInntektClient,
    saksbehandler: BrukerOgToken,
): Dokument =
    lastAInntektDok(
        periode = periode,
        aInntektClient = aInntektClient,
        filter = AInntektFilter.`8-30`,
        fomMinus = 13,
        tomMinus = 1,
        saksbehandler = saksbehandler,
    )

fun DokumentInnhentingDaoer.lastAInntektBeregningsgrunnlag(
    periode: Saksbehandlingsperiode,
    aInntektClient: AInntektClient,
    saksbehandler: BrukerOgToken,
): Dokument =
    lastAInntektDok(
        periode = periode,
        aInntektClient = aInntektClient,
        filter = AInntektFilter.`8-28`,
        fomMinus = 4,
        tomMinus = 1,
        saksbehandler = saksbehandler,
    )

private fun doktypeFraFilter(filter: AInntektFilter): String =
    when (filter) {
        AInntektFilter.`8-28` -> DokumentType.aInntekt828
        AInntektFilter.`8-30` -> DokumentType.aInntekt830
    }

private fun DokumentInnhentingDaoer.lastAInntektDok(
    periode: Saksbehandlingsperiode,
    aInntektClient: AInntektClient,
    filter: AInntektFilter,
    fomMinus: Long,
    tomMinus: Long,
    saksbehandler: BrukerOgToken,
): Dokument {
    val fnr = personDao.hentNaturligIdent(periode.spilleromPersonId)
    val skjæringstidspunkt = periode.skjæringstidspunkt ?: throw IllegalStateException("Skjæringstidspunkt må være satt for å hente inntekt")

    // TODO: Bør vi ha litt slack på fom/tom?
    val fom = skjæringstidspunkt.yearMonth.minusMonths(fomMinus)
    val tom = skjæringstidspunkt.yearMonth.minusMonths(tomMinus)

    val dokType = doktypeFraFilter(filter)
    val forespurteDataNøkkel = "${fom}_$tom"

    val alleredeLagret =
        dokumentDao.finnDokumentForForespurteData(
            saksbehandlingsperiodeId = periode.id,
            dokumentType = dokType,
            forespurteData = forespurteDataNøkkel,
        )
    if (alleredeLagret != null) {
        return alleredeLagret
    }

    return runBlocking {
        aInntektClient
            .hentInntekterForMedSporing(
                fnr = fnr,
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
