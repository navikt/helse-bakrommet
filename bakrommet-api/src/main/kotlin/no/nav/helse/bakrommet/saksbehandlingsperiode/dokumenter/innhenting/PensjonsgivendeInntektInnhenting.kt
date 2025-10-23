package no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.innhenting

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import no.nav.helse.bakrommet.auth.SpilleromBearerToken
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.Dokument
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.DokumentType
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.joinSigrunResponserTilEttDokument
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.HentPensjonsgivendeInntektResponse
import no.nav.helse.bakrommet.sigrun.SigrunClient
import no.nav.helse.bakrommet.util.objectMapper
import no.nav.helse.bakrommet.util.serialisertTilString

fun DokumentInnhentingDaoer.lastSigrunDokument(
    periode: Saksbehandlingsperiode,
    saksbehandlerToken: SpilleromBearerToken,
    sigrunClient: SigrunClient,
): Dokument {
    val fnr = personDao.hentNaturligIdent(periode.spilleromPersonId)
    val skjæringstidspunkt = periode.skjæringstidspunkt ?: throw IllegalStateException("Skjæringstidspunkt må være satt for å hente pensjonsgivende inntekt")
    val senesteÅrTom = skjæringstidspunkt.year - 1
    val antallÅrBakover = 3
    val dokType = DokumentType.pensjonsgivendeinntekt
    val forespurteDataNøkkel = "${senesteÅrTom}_$antallÅrBakover"
    val alleredeLagret =
        dokumentDao.finnDokumentForForespurteData(
            saksbehandlingsperiodeId = periode.id,
            dokumentType = dokType,
            forespurteData = forespurteDataNøkkel,
        )
    if (alleredeLagret != null) {
        return alleredeLagret
    }
    val (sigrundata, kildespor) =
        runBlocking {
            sigrunClient
                .hentPensjonsgivendeInntektForÅrSenestOgAntallÅrBakover(
                    fnr,
                    senesteÅrTom,
                    antallÅrBakover,
                    saksbehandlerToken,
                ).joinSigrunResponserTilEttDokument()
        }

    return dokumentDao.opprettDokument(
        Dokument(
            dokumentType = dokType,
            eksternId = null,
            innhold = sigrundata.serialisertTilString(),
            sporing = kildespor,
            opprettetForBehandling = periode.id,
            forespurteData = forespurteDataNøkkel,
        ),
    )
}

fun Dokument.somPensjonsgivendeInntekt(): List<HentPensjonsgivendeInntektResponse> {
    require(dokumentType == DokumentType.pensjonsgivendeinntekt)
    return objectMapper.readValue<List<HentPensjonsgivendeInntektResponse>>(innhold)
}
