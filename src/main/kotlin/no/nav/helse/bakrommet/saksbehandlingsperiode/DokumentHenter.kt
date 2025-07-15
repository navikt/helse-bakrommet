package no.nav.helse.bakrommet.saksbehandlingsperiode

import no.nav.helse.bakrommet.ainntekt.AInntektClient
import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.auth.SpilleromBearerToken
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadBackendClient
import no.nav.helse.bakrommet.util.logg
import no.nav.helse.bakrommet.util.serialisertTilString
import java.time.YearMonth
import java.util.*

class DokumentHenter(
    private val personDao: PersonDao,
    private val saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao,
    private val dokumentDao: DokumentDao,
    private val soknadClient: SykepengesoknadBackendClient,
    private val aInntektClient: AInntektClient,
) {
    fun hentDokumenterFor(ref: SaksbehandlingsperiodeReferanse): List<Dokument> {
        val periode = saksbehandlingsperiodeDao.hentPeriode(ref)
        return dokumentDao.hentDokumenterFor(periode.id)
    }

    fun hentDokument(
        ref: SaksbehandlingsperiodeReferanse,
        dokumentId: UUID,
    ): Dokument? {
        val periode = saksbehandlingsperiodeDao.hentPeriode(ref)
        val dok = dokumentDao.hentDokument(dokumentId)
        if (dok != null) {
            if (dok.opprettetForBehandling != periode.id) {
                throw InputValideringException("Dokumentet tilhører ikke behandlingen")
            }
        }
        return dok
    }

    suspend fun hentOgLagreSøknader(
        saksbehandlingsperiodeId: UUID,
        søknadsIder: List<UUID>,
        spilleromBearerToken: SpilleromBearerToken,
    ): List<Dokument> {
        if (søknadsIder.isEmpty()) return emptyList()
        val periode = saksbehandlingsperiodeDao.finnSaksbehandlingsperiode(saksbehandlingsperiodeId)
        requireNotNull(periode) { "Fant ikke saksbehandlingsperiode med id=$saksbehandlingsperiodeId" }

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

        return søknader
    }

    suspend fun hentOgLagreAInntekt(
        ref: SaksbehandlingsperiodeReferanse,
        fom: YearMonth,
        tom: YearMonth,
        saksbehandler: BrukerOgToken,
    ): Dokument {
        val periode = saksbehandlingsperiodeDao.hentPeriode(ref)
        val fnr = personDao.finnNaturligIdent(periode.spilleromPersonId)!!
        return aInntektClient.hentInntekterForMedSporing(
            fnr = fnr,
            maanedFom = fom,
            maanedTom = tom,
            saksbehandlerToken = saksbehandler.token,
        ).let { (inntekter, kildespor) ->
            // TODO: Sjekk om akkurat samme dokument med samme innhold allerede eksisterer ?
            dokumentDao.opprettDokument(
                Dokument(
                    dokumentType = DokumentType.aInntekt828,
                    eksternId = null,
                    innhold = inntekter.serialisertTilString(),
                    request = kildespor,
                    opprettetForBehandling = periode.id,
                ),
            )
        }
    }
}
