package no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.bakrommet.aareg.AARegClient
import no.nav.helse.bakrommet.ainntekt.AInntektClient
import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeReferanse
import no.nav.helse.bakrommet.saksbehandlingsperiode.erSaksbehandlerPåSaken
import no.nav.helse.bakrommet.saksbehandlingsperiode.hentPeriode
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
import java.time.YearMonth
import java.util.*

class DokumentHenter(
    private val personDao: PersonDao,
    private val saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao,
    private val dokumentDao: DokumentDao,
    private val soknadClient: SykepengesoknadBackendClient,
    private val aInntektClient: AInntektClient,
    private val aaRegClient: AARegClient,
    private val sigrunClient: SigrunClient,
) {
    fun hentDokumenterFor(ref: SaksbehandlingsperiodeReferanse): List<Dokument> {
        val periode = saksbehandlingsperiodeDao.hentPeriode(ref, krav = null)
        return dokumentDao.hentDokumenterFor(periode.id)
    }

    fun hentDokument(
        ref: SaksbehandlingsperiodeReferanse,
        dokumentId: UUID,
    ): Dokument? {
        val periode = saksbehandlingsperiodeDao.hentPeriode(ref, krav = null)
        val dok = dokumentDao.hentDokument(dokumentId)
        if (dok != null) {
            if (dok.opprettetForBehandling != periode.id) {
                throw InputValideringException("Dokumentet tilhører ikke behandlingen")
            }
        }
        return dok
    }

    suspend fun hentOgLagreSøknader(
        ref: SaksbehandlingsperiodeReferanse,
        søknadsIder: List<UUID>,
        saksbehandler: BrukerOgToken,
    ): List<Dokument> {
        if (søknadsIder.isEmpty()) return emptyList()
        val periode = saksbehandlingsperiodeDao.hentPeriode(ref, krav = saksbehandler.bruker.erSaksbehandlerPåSaken())

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

        return søknader
    }

    suspend fun hentOgLagreAInntekt(
        ref: SaksbehandlingsperiodeReferanse,
        fom: YearMonth,
        tom: YearMonth,
        saksbehandler: BrukerOgToken,
    ): Dokument {
        val periode = saksbehandlingsperiodeDao.hentPeriode(ref, krav = saksbehandler.bruker.erSaksbehandlerPåSaken())
        val fnr = personDao.hentNaturligIdent(periode.spilleromPersonId)
        return aInntektClient
            .hentInntekterForMedSporing(
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
                        sporing = kildespor,
                        opprettetForBehandling = periode.id,
                    ),
                )
            }
    }

    suspend fun hentOgLagreArbeidsforhold(
        ref: SaksbehandlingsperiodeReferanse,
        saksbehandler: BrukerOgToken,
    ): Dokument {
        val periode = saksbehandlingsperiodeDao.hentPeriode(ref, krav = saksbehandler.bruker.erSaksbehandlerPåSaken())
        val fnr = personDao.hentNaturligIdent(periode.spilleromPersonId)
        logg.info("Henter aareg for periode={}", periode.id)
        return aaRegClient
            .hentArbeidsforholdForMedSporing(
                fnr = fnr,
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

    suspend fun hentOgLagrePensjonsgivendeInntekt(
        ref: SaksbehandlingsperiodeReferanse,
        senesteÅrTom: Int,
        antallÅrBakover: Int,
        saksbehandler: BrukerOgToken,
    ): Dokument {
        val periode = saksbehandlingsperiodeDao.hentPeriode(ref, krav = saksbehandler.bruker.erSaksbehandlerPåSaken())
        val fnr = personDao.hentNaturligIdent(periode.spilleromPersonId)

        return sigrunClient
            .hentPensjonsgivendeInntektForÅrSenestOgAntallÅrBakover(
                fnr = fnr,
                senesteÅrTom = senesteÅrTom,
                antallÅrBakover = antallÅrBakover,
                saksbehandlerToken = saksbehandler.token,
            ).let { reponsMedSporing ->
                reponsMedSporing.joinSigrunResponserTilEttDokument().let { (innhold, kildespor) ->
                    dokumentDao.opprettDokument(
                        Dokument(
                            dokumentType = DokumentType.pensjonsgivendeinntekt,
                            eksternId = null,
                            innhold = innhold.serialisertTilString(),
                            sporing = kildespor,
                            opprettetForBehandling = periode.id,
                        ),
                    )
                }
            }
    }
}

fun List<PensjonsgivendeInntektÅrMedSporing>.joinSigrunResponserTilEttDokument(): Pair<JsonNode, Kildespor> {
    require(this.isNotEmpty())
    val data = map { it.data() }
    val sporingMap = mapOf(*map { it.data().inntektsaar() to it.sporing().kilde }.toTypedArray())
    return data.toJsonNode() to Kildespor(sporingMap.toString())
}
