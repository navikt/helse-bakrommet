package no.nav.helse.bakrommet.saksbehandlingsperiode

import no.nav.helse.bakrommet.ainntekt.AInntektClient
import no.nav.helse.bakrommet.auth.SpilleromBearerToken
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadBackendClient
import no.nav.helse.bakrommet.util.*
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

class DokumentHenter(
    private val personDao: PersonDao,
    private val saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao,
    private val dokumentDao: DokumentDao,
    private val soknadClient: SykepengesoknadBackendClient,
    private val aInntektClient: AInntektClient,
) {
    companion object {
        object DokumentType {
            val søknad = "søknad"
            val aInntekt828 = "ainntekt828"
        }
    }

    suspend fun hentOgLagreSøknaderOgInntekter(
        saksbehandlingsperiodeId: UUID,
        søknadsIder: List<UUID>,
        spilleromBearerToken: SpilleromBearerToken,
    ): List<Dokument> {
        if (søknadsIder.isEmpty()) return emptyList()
        val periode = saksbehandlingsperiodeDao.finnSaksbehandlingsperiode(saksbehandlingsperiodeId)
        requireNotNull(periode) { "Fant ikke saksbehandlingsperiode med id=$saksbehandlingsperiodeId" }

        val fnr = personDao.finnNaturligIdent(periode.spilleromPersonId)!!

        // TODO: Transaksjon ? / Tilrettelegg for å kunne fullføre innhenting som feiler halvveis inni løpet ?

        val søknader: List<Dokument> =
            søknadsIder.map { søknadId ->
                logg.info("Henter søknad med id={} for periode={}", søknadId, periode.id)
                soknadClient.hentSoknadMedSporing(
                    saksbehandlerToken = spilleromBearerToken,
                    id = søknadId.toString(),
                ).let { (søknadDto, kildespor) ->
                    dokumentDao.opprettDokument(
                        Dokument(
                            dokumentType = DokumentType.søknad,
                            eksternId = søknadId.toString(),
                            innhold = søknadDto.serialisertTilString(),
                            request = kildespor,
                            opprettetForBehandling = saksbehandlingsperiodeId,
                        ),
                    )
                }
            }

        logg.info("Henter inntekter for periode={}", periode.id)
        val inntekt: Dokument =
            aInntektClient.hentInntekterForMedSporing(
                fnr = fnr,
                maanedFom = YearMonth.from(periode.fom.minusMonths(13)),
                maanedTom = YearMonth.from(LocalDate.now()),
                saksbehandlerToken = spilleromBearerToken,
            ).let { (inntekter, kildespor) ->
                dokumentDao.opprettDokument(
                    Dokument(
                        dokumentType = DokumentType.aInntekt828,
                        eksternId = null,
                        innhold = inntekter.serialisertTilString(),
                        request = kildespor,
                        opprettetForBehandling = saksbehandlingsperiodeId,
                    ),
                )
            }

        return søknader + inntekt
    }
}
