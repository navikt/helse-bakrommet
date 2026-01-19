package no.nav.helse.bakrommet.behandling.dokumenter

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.bakrommet.Kildespor
import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.behandling.BehandlingReferanse
import no.nav.helse.bakrommet.behandling.dokumenter.innhenting.lastAInntektBeregningsgrunnlag
import no.nav.helse.bakrommet.behandling.dokumenter.innhenting.lastAInntektSammenlikningsgrunnlag
import no.nav.helse.bakrommet.behandling.dokumenter.innhenting.lastSigrunDokument
import no.nav.helse.bakrommet.behandling.erSaksbehandlerPåSaken
import no.nav.helse.bakrommet.behandling.hentPeriode
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.infrastruktur.provider.ArbeidsforholdProvider
import no.nav.helse.bakrommet.infrastruktur.provider.InntekterProvider
import no.nav.helse.bakrommet.infrastruktur.provider.PensjonsgivendeInntektProvider
import no.nav.helse.bakrommet.infrastruktur.provider.PensjonsgivendeInntektÅrMedSporing
import no.nav.helse.bakrommet.infrastruktur.provider.SykepengesøknadProvider
import no.nav.helse.bakrommet.infrastruktur.provider.data
import no.nav.helse.bakrommet.infrastruktur.provider.inntektsaar
import no.nav.helse.bakrommet.infrastruktur.provider.sporing
import no.nav.helse.bakrommet.logg
import no.nav.helse.bakrommet.serialisertTilString
import no.nav.helse.bakrommet.toJsonNode
import java.util.*

class DokumentHenter(
    val db: DbDaoer<AlleDaoer>,
    private val soknadClient: SykepengesøknadProvider,
    private val inntekterProvider: InntekterProvider,
    private val arbeidsforholdProvider: ArbeidsforholdProvider,
    private val pensjonsgivendeInntektProvider: PensjonsgivendeInntektProvider,
) {
    suspend fun hentDokumenterFor(ref: BehandlingReferanse): List<Dokument> =
        db.nonTransactional {
            val periode = behandlingDao.hentPeriode(ref, krav = null, måVæreUnderBehandling = false)
            dokumentDao.hentDokumenterFor(periode.id)
        }

    suspend fun hentDokument(
        ref: BehandlingReferanse,
        dokumentId: UUID,
    ): Dokument? =
        db.nonTransactional {
            val periode = behandlingDao.hentPeriode(ref, krav = null)
            val dok = dokumentDao.hentDokument(dokumentId)
            if (dok != null) {
                if (dok.opprettetForBehandling != periode.id) {
                    throw InputValideringException("Dokumentet tilhører ikke behandlingen")
                }
            }
            dok
        }

    suspend fun hentOgLagreSøknader(
        ref: BehandlingReferanse,
        søknadsIder: List<UUID>,
        saksbehandler: BrukerOgToken,
    ): List<Dokument> =
        db.nonTransactional {
            if (søknadsIder.isEmpty()) return@nonTransactional emptyList()
            val periode =
                behandlingDao.hentPeriode(ref, krav = saksbehandler.bruker.erSaksbehandlerPåSaken())

            // TODO: Transaksjon ? / Tilrettelegg for å kunne fullføre innhenting som feiler halvveis inni løpet ?

            val søknader: List<Dokument> =
                søknadsIder.map { søknadId ->
                    logg.info("Henter søknad med id={} for periode={}", søknadId, periode.id)
                    soknadClient
                        .hentSoknadMedSporing(
                            saksbehandlerToken = saksbehandler.token,
                            id = søknadId.toString(),
                        ).let { (søknadDto, kildespor) ->
                            dokumentDao.opprettDokument(
                                Dokument(
                                    dokumentType = DokumentType.søknad,
                                    eksternId = søknadId.toString(),
                                    innhold = søknadDto.serialisertTilString(),
                                    sporing = kildespor,
                                    opprettetForBehandling = periode.id,
                                ),
                            )
                        }
                }

            return@nonTransactional søknader
        }

    suspend fun hentOgLagreAInntekt828(
        ref: BehandlingReferanse,
        saksbehandler: BrukerOgToken,
    ): Dokument =
        db.transactional {
            val behandling =
                behandlingRepository.hent(BehandlingId(ref.behandlingId))
            if (!saksbehandler.bruker.erTildelt(behandling)) {
                error("Bruker er ikke saksbehandler på saken")
            }
            if (!behandling.erÅpenForEndringer()) {
                error("Behandling er ikke åpen for endringer")
            }

            lastAInntektBeregningsgrunnlag(
                behandling = behandling,
                inntekterProvider = inntekterProvider,
                saksbehandler = saksbehandler,
            )
        }

    suspend fun hentOgLagreAInntekt830(
        ref: BehandlingReferanse,
        saksbehandler: BrukerOgToken,
    ): Dokument =
        db.nonTransactional {
            val behandling =
                behandlingRepository.hent(BehandlingId(ref.behandlingId))
            if (!saksbehandler.bruker.erTildelt(behandling)) {
                error("Bruker er ikke saksbehandler på saken")
            }
            if (!behandling.erÅpenForEndringer()) {
                error("Behandling er ikke åpen for endringer")
            }

            return@nonTransactional lastAInntektSammenlikningsgrunnlag(
                behandling = behandling,
                inntekterProvider = inntekterProvider,
                saksbehandler = saksbehandler,
            )
        }

    suspend fun hentOgLagreArbeidsforhold(
        ref: BehandlingReferanse,
        saksbehandler: BrukerOgToken,
    ): Dokument {
        return db.nonTransactional {
            val periode =
                behandlingDao.hentPeriode(ref, krav = saksbehandler.bruker.erSaksbehandlerPåSaken())
            logg.info("Henter aareg for periode={}", periode.id)
            return@nonTransactional arbeidsforholdProvider
                .hentArbeidsforholdForMedSporing(
                    fnr = periode.naturligIdent.value,
                    saksbehandlerToken = saksbehandler.token,
                ).let { (arbeidsforholdRes, kildespor) ->
                    // TODO: Sjekk om akkurat samme dokument med samme innhold allerede eksisterer ?
                    dokumentDao.opprettDokument(
                        Dokument(
                            dokumentType = DokumentType.arbeidsforhold,
                            eksternId = null,
                            innhold = arbeidsforholdRes.serialisertTilString(),
                            sporing = kildespor,
                            opprettetForBehandling = periode.id,
                        ),
                    )
                }
        }
    }

    suspend fun hentOgLagrePensjonsgivendeInntekt(
        ref: BehandlingReferanse,
        saksbehandler: BrukerOgToken,
    ): Dokument =
        db.transactional {
            val behandling =
                behandlingRepository.hent(BehandlingId(ref.behandlingId))
            if (!saksbehandler.bruker.erTildelt(behandling)) {
                error("Bruker er ikke saksbehandler på saken")
            }
            if (!behandling.erÅpenForEndringer()) {
                error("Behandling er ikke åpen for endringer")
            }

            lastSigrunDokument(
                behandling = behandling,
                saksbehandlerToken = saksbehandler.token,
                pensjonsgivendeInntektProvider = pensjonsgivendeInntektProvider,
            )
        }
}

fun List<PensjonsgivendeInntektÅrMedSporing>.joinSigrunResponserTilEttDokument(): Pair<JsonNode, Kildespor> {
    require(this.isNotEmpty())
    val data = map { it.data() }
    val sporingMap = mapOf(*map { it.data().inntektsaar() to it.sporing().kilde }.toTypedArray())
    return data.toJsonNode() to Kildespor(sporingMap.toString())
}
