package no.nav.helse.bakrommet.behandling.dokumenter.innhenting

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.behandling.BehandlingDbRecord
import no.nav.helse.bakrommet.behandling.dokumenter.Dokument
import no.nav.helse.bakrommet.behandling.dokumenter.DokumentType
import no.nav.helse.bakrommet.clients.InntektsmeldingProvider
import no.nav.helse.bakrommet.util.asJsonNode
import no.nav.helse.bakrommet.util.objectMapper
import no.nav.helse.bakrommet.util.serialisertTilString
import no.nav.inntektsmeldingkontrakt.Inntektsmelding

fun DokumentInnhentingDaoer.lastInntektsmeldingDokument(
    periode: BehandlingDbRecord,
    inntektsmeldingId: String,
    inntektsmeldingClient: InntektsmeldingProvider,
    saksbehandler: BrukerOgToken,
): Dokument {
    val dokType = DokumentType.inntektsmelding
    val alleredeLagret =
        dokumentDao.finnDokumentMedEksternId(
            behandlingId = periode.id,
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
    require(
        periode.naturligIdent.naturligIdent == inntektsmelding["arbeidstakerFnr"].asText(),
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

fun JsonNode.somInntektsmeldingObjekt(): Inntektsmelding = objectMapper.readValue(this.serialisertTilString())

fun JsonNode.somInntektsmeldingObjektListe(): List<Inntektsmelding> = objectMapper.readValue(this.serialisertTilString())
