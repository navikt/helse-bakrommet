package no.nav.helse.bakrommet.behandling.dokumenter

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.runBlocking
import no.nav.helse.bakrommet.aareg.AARegClient
import no.nav.helse.bakrommet.ainntekt.AInntektClient
import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.behandling.BehandlingReferanse
import no.nav.helse.bakrommet.behandling.dokumenter.innhenting.DokumentInnhentingDaoer
import no.nav.helse.bakrommet.behandling.dokumenter.innhenting.lastAInntektBeregningsgrunnlag
import no.nav.helse.bakrommet.behandling.dokumenter.innhenting.lastAInntektSammenlikningsgrunnlag
import no.nav.helse.bakrommet.behandling.dokumenter.innhenting.lastSigrunDokument
import no.nav.helse.bakrommet.behandling.erSaksbehandlerPåSaken
import no.nav.helse.bakrommet.behandling.hentPeriode
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.sigrun.PensjonsgivendeInntektÅrMedSporing
import no.nav.helse.bakrommet.sigrun.SigrunClient
import no.nav.helse.bakrommet.sigrun.data
import no.nav.helse.bakrommet.sigrun.inntektsaar
import no.nav.helse.bakrommet.sigrun.sporing
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadBackendClient
import no.nav.helse.bakrommet.util.Kildespor
import no.nav.helse.bakrommet.util.logg
import no.nav.helse.bakrommet.util.serialisertTilString
import no.nav.helse.bakrommet.util.toJsonNode
import java.util.*

class DokumentHenter(
    val db: DbDaoer<DokumentInnhentingDaoer>,
    private val soknadClient: SykepengesoknadBackendClient,
    private val aInntektClient: AInntektClient,
    private val aaRegClient: AARegClient,
    private val sigrunClient: SigrunClient,
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
                runBlocking {
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
                }

            return@nonTransactional søknader
        }

    suspend fun hentOgLagreAInntekt828(
        ref: BehandlingReferanse,
        saksbehandler: BrukerOgToken,
    ): Dokument =
        db.nonTransactional {
            val periode =
                behandlingDao.hentPeriode(ref, krav = saksbehandler.bruker.erSaksbehandlerPåSaken())

            return@nonTransactional lastAInntektBeregningsgrunnlag(
                periode = periode,
                aInntektClient = aInntektClient,
                saksbehandler = saksbehandler,
            )
        }

    suspend fun hentOgLagreAInntekt830(
        ref: BehandlingReferanse,
        saksbehandler: BrukerOgToken,
    ): Dokument =
        db.nonTransactional {
            val periode =
                behandlingDao.hentPeriode(ref, krav = saksbehandler.bruker.erSaksbehandlerPåSaken())

            return@nonTransactional lastAInntektSammenlikningsgrunnlag(
                periode = periode,
                aInntektClient = aInntektClient,
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
            return@nonTransactional aaRegClient
                .hentArbeidsforholdForMedSporing(
                    fnr = periode.naturligIdent.naturligIdent,
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
        db.nonTransactional {
            val periode = behandlingDao.hentPeriode(ref, krav = saksbehandler.bruker.erSaksbehandlerPåSaken())

            return@nonTransactional lastSigrunDokument(
                periode = periode,
                saksbehandlerToken = saksbehandler.token,
                sigrunClient = sigrunClient,
            )
        }
}

fun List<PensjonsgivendeInntektÅrMedSporing>.joinSigrunResponserTilEttDokument(): Pair<JsonNode, Kildespor> {
    require(this.isNotEmpty())
    val data = map { it.data() }
    val sporingMap = mapOf(*map { it.data().inntektsaar() to it.sporing().kilde }.toTypedArray())
    return data.toJsonNode() to Kildespor(sporingMap.toString())
}
