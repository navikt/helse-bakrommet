package no.nav.helse.bakrommet.behandling.dokumenter.innhenting

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.runBlocking
import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.behandling.Behandling
import no.nav.helse.bakrommet.behandling.dokumenter.Dokument
import no.nav.helse.bakrommet.behandling.dokumenter.DokumentType
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingClient
import no.nav.helse.bakrommet.util.asJsonNode
import no.nav.helse.bakrommet.util.serialisertTilString

fun DokumentInnhentingDaoer.lastInntektsmeldingDokument(
    periode: Behandling,
    inntektsmeldingId: String,
    inntektsmeldingClient: InntektsmeldingClient,
    saksbehandler: BrukerOgToken,
): Dokument {
    val dokType = DokumentType.inntektsmelding
    val alleredeLagret =
        dokumentDao.finnDokumentMedEksternId(
            saksbehandlingsperiodeId = periode.id,
            dokumentType = dokType,
            eksternId = inntektsmeldingId,
        )
    if (alleredeLagret != null) {
        return alleredeLagret
    }
    val (inntektsmelding, kildespor) =
        runBlocking {
            inntektsmeldingClient.hentInntektsmeldingMedSporing(
                inntektsmeldingId = inntektsmeldingId,
                saksbehandlerToken = saksbehandler.token,
            )
        }
    val fnr = personDao.hentNaturligIdent(periode.spilleromPersonId)
    require(
        fnr == inntektsmelding["arbeidstakerFnr"].asText(),
        { "arbeidstakerFnr i inntektsmelding må være lik sykmeldts naturligIdent" },
    )
    return dokumentDao.opprettDokument(
        Dokument(
            dokumentType = dokType,
            eksternId = inntektsmeldingId,
            innhold = inntektsmelding.serialisertTilString(),
            sporing = kildespor,
            opprettetForBehandling = periode.id,
        ),
    )
}

fun Dokument.somInntektsmelding(): JsonNode {
    require(dokumentType == DokumentType.inntektsmelding)
    return this.innhold.asJsonNode()
}
