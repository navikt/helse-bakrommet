package no.nav.helse.bakrommet.behandling.dokumenter.innhenting

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import no.nav.helse.bakrommet.auth.AccessToken
import no.nav.helse.bakrommet.behandling.BehandlingDbRecord
import no.nav.helse.bakrommet.behandling.dokumenter.Dokument
import no.nav.helse.bakrommet.behandling.dokumenter.DokumentType
import no.nav.helse.bakrommet.behandling.dokumenter.joinSigrunResponserTilEttDokument
import no.nav.helse.bakrommet.behandling.inntekter.HentPensjonsgivendeInntektResponse
import no.nav.helse.bakrommet.infrastruktur.provider.PensjonsgivendeInntektProvider
import no.nav.helse.bakrommet.util.objectMapper
import no.nav.helse.bakrommet.util.serialisertTilString

fun DokumentInnhentingDaoer.lastSigrunDokument(
    periode: BehandlingDbRecord,
    saksbehandlerToken: AccessToken,
    pensjonsgivendeInntektProvider: PensjonsgivendeInntektProvider,
): Dokument {
    val skjæringstidspunkt = periode.skjæringstidspunkt
    val senesteÅrTom = skjæringstidspunkt.year - 1
    val antallÅrBakover = 3
    val dokType = DokumentType.pensjonsgivendeinntekt
    val forespurteDataNøkkel = "${senesteÅrTom}_$antallÅrBakover"
    val alleredeLagret =
        dokumentDao.finnDokumentForForespurteData(
            behandlingId = periode.id,
            dokumentType = dokType,
            forespurteData = forespurteDataNøkkel,
        )
    if (alleredeLagret != null) {
        return alleredeLagret
    }
    val (sigrundata, kildespor) =
        runBlocking {
            pensjonsgivendeInntektProvider
                .hentPensjonsgivendeInntektForÅrSenestOgAntallÅrBakover(
                    periode.naturligIdent.value,
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
