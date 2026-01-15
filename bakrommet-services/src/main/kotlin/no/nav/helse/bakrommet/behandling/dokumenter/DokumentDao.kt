package no.nav.helse.bakrommet.behandling.dokumenter

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.bakrommet.Kildespor
import no.nav.helse.bakrommet.util.*
import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
import java.time.Instant
import java.util.*

object DokumentType {
    val søknad = "søknad"
    val inntektsmelding = "inntektsmelding"
    val aInntekt828 = "ainntekt828"
    val aInntekt830 = "ainntekt830"
    val arbeidsforhold = "arbeidsforhold"
    val pensjonsgivendeinntekt = "pensjonsgivendeinntekt"
}

data class Dokument(
    val id: UUID = UUID.randomUUID(),
    val dokumentType: String,
    val eksternId: String?,
    val innhold: String,
    val opprettet: Instant = Instant.now(),
    val sporing: Kildespor,
    val forespurteData: String? = null,
    val opprettetForBehandling: UUID,
) {
    fun somSøknad(): SykepengesoknadDTO {
        check(dokumentType == DokumentType.søknad)
        return objectMapper.readValue<SykepengesoknadDTO>(innhold)
    }
}

interface DokumentDao {
    fun finnDokumentMedEksternId(
        behandlingId: UUID,
        dokumentType: String,
        eksternId: String,
    ): Dokument?

    fun finnDokumentForForespurteData(
        behandlingId: UUID,
        dokumentType: String,
        forespurteData: String,
    ): Dokument?

    fun opprettDokument(dokument: Dokument): Dokument

    fun hentDokument(id: UUID): Dokument?

    fun hentDokumenterFor(behandlingId: UUID): List<Dokument>
}
