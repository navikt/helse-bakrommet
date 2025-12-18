package no.nav.helse.bakrommet.behandling

import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.behandling.BehandlingStatus.GODKJENT
import no.nav.helse.bakrommet.behandling.BehandlingStatus.REVURDERT
import no.nav.helse.bakrommet.behandling.BehandlingStatus.TIL_BESLUTNING
import no.nav.helse.bakrommet.behandling.BehandlingStatus.UNDER_BEHANDLING
import no.nav.helse.bakrommet.behandling.BehandlingStatus.UNDER_BESLUTNING
import no.nav.helse.bakrommet.behandling.SaksbehandlingsperiodeEndringType.REVURDERING_STARTET
import no.nav.helse.bakrommet.behandling.beregning.Beregningsdaoer
import no.nav.helse.bakrommet.behandling.beregning.beregnSykepengegrunnlagOgUtbetaling
import no.nav.helse.bakrommet.behandling.beregning.beregnUtbetaling
import no.nav.helse.bakrommet.behandling.dagoversikt.initialiserDager
import no.nav.helse.bakrommet.behandling.dagoversikt.skapDagoversiktFraSoknader
import no.nav.helse.bakrommet.behandling.dokumenter.Dokument
import no.nav.helse.bakrommet.behandling.dokumenter.DokumentDao
import no.nav.helse.bakrommet.behandling.dokumenter.DokumentHenter
import no.nav.helse.bakrommet.behandling.vilkaar.Kode
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetDbRecord
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.*
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.kafka.SaksbehandlingsperiodeKafkaDtoDaoer
import no.nav.helse.bakrommet.kafka.leggTilOutbox
import no.nav.helse.bakrommet.person.NaturligIdent
import no.nav.helse.bakrommet.util.logg
import no.nav.helse.flex.sykepengesoknad.kafka.ArbeidssituasjonDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

interface BehandlingServiceDaoer :
    SaksbehandlingsperiodeKafkaDtoDaoer,
    Beregningsdaoer {
    val behandlingEndringerDao: BehandlingEndringerDao
    val dokumentDao: DokumentDao
}

data class BehandlingReferanse(
    val naturligIdent: NaturligIdent,
    val behandlingId: UUID,
)

fun Behandling.somReferanse() =
    BehandlingReferanse(
        naturligIdent = this.naturligIdent,
        behandlingId = this.id,
    )

class BehandlingService(
    private val db: DbDaoer<BehandlingServiceDaoer>,
    private val dokumentHenter: DokumentHenter,
) {
    suspend fun hentAlleSaksbehandlingsperioder() = db.nonTransactional { behandlingDao.hentAlleBehandlinger() }

    suspend fun hentPeriode(ref: BehandlingReferanse) = db.nonTransactional { behandlingDao.hentPeriode(ref, krav = null) }

    suspend fun opprettNyBehandling(
        naturligIdent: NaturligIdent,
        fom: LocalDate,
        tom: LocalDate,
        søknader: Set<UUID>,
        saksbehandler: BrukerOgToken,
        id: UUID = UUID.randomUUID(),
    ): Behandling {
        if (fom.isAfter(tom)) throw InputValideringException("Fom-dato kan ikke være etter tom-dato")
        var nyPeriode =
            Behandling(
                id = id,
                naturligIdent = naturligIdent,
                opprettet = OffsetDateTime.now(),
                opprettetAvNavIdent = saksbehandler.bruker.navIdent,
                opprettetAvNavn = saksbehandler.bruker.navn,
                fom = fom,
                tom = tom,
                skjæringstidspunkt = fom,
                revurdererSaksbehandlingsperiodeId = null,
            )

        var tidligerePeriodeInntilNyPeriode: Behandling? = null

        db.transactional {
            val perioder = behandlingDao.finnBehandlingerForNaturligIdent(naturligIdent)

            if (perioder.any { it.fom <= tom && it.tom >= fom }) {
                throw InputValideringException("Angitte datoer overlapper med en eksisterende periode")
            }

            tidligerePeriodeInntilNyPeriode =
                perioder
                    .filter {
                        it.status in
                            listOf(
                                UNDER_BEHANDLING, // TODO denne bør egentlig bort, vi bør også sikre kun 1 UNDER_BEHANDLING per person
                                TIL_BESLUTNING,
                                UNDER_BESLUTNING,
                                GODKJENT,
                            )
                    }.find { it.tom.plusDays(1).isEqual(fom) }

            tidligerePeriodeInntilNyPeriode?.let {
                nyPeriode =
                    nyPeriode.copy(
                        skjæringstidspunkt = it.skjæringstidspunkt,
                        sykepengegrunnlagId = it.sykepengegrunnlagId,
                    )
            }

            behandlingDao.opprettPeriode(nyPeriode)
            behandlingEndringerDao.leggTilEndring(
                nyPeriode.endring(
                    endringType = SaksbehandlingsperiodeEndringType.STARTET,
                    saksbehandler = saksbehandler.bruker,
                ),
            )
            tidligerePeriodeInntilNyPeriode?.let {
                vurdertVilkårDao.hentVilkårsvurderinger(it.id).forEach { v ->
                    vurdertVilkårDao.leggTil(
                        nyPeriode,
                        Kode(v.kode),
                        v.vurdering,
                    )
                }
            }
            leggTilOutbox(nyPeriode)
        }

        val søknader =
            if (søknader.isNotEmpty()) {
                dokumentHenter.hentOgLagreSøknader(
                    nyPeriode.somReferanse(),
                    søknader.toList(),
                    saksbehandler,
                )
            } else {
                emptyList()
            }
// TODO kan vi få alt i samme trans. ?
        db.transactional {
            val tidligereYrkesaktiviteter =
                tidligerePeriodeInntilNyPeriode
                    ?.let { yrkesaktivitetDao.hentYrkesaktiviteterDbRecord(it) }
                    ?: emptyList()

            val (yrkesaktiviteter, gammelTilNyIdMap) =
                lagYrkesaktiviteter(
                    sykepengesoknader = søknader,
                    behandling = nyPeriode,
                    tidligereYrkesaktiviteter = tidligereYrkesaktiviteter,
                )
            yrkesaktiviteter.forEach { yrkesaktivitet ->
                // Konverter til sealed class og bruk den nye DAO-funksjonen
                yrkesaktivitetDao.opprettYrkesaktivitet(
                    id = yrkesaktivitet.id,
                    kategorisering = yrkesaktivitet.kategorisering,
                    dagoversikt = yrkesaktivitet.dagoversikt,
                    saksbehandlingsperiodeId = yrkesaktivitet.saksbehandlingsperiodeId,
                    opprettet = yrkesaktivitet.opprettet,
                    generertFraDokumenter = yrkesaktivitet.generertFraDokumenter,
                    perioder = yrkesaktivitet.perioder,
                    inntektData = yrkesaktivitet.inntektData,
                    refusjonsdata = yrkesaktivitet.refusjon,
                )
            }

            tidligerePeriodeInntilNyPeriode?.let {
                it.sykepengegrunnlagId?.let { spgid ->
                    behandlingDao.oppdaterSykepengegrunnlagId(nyPeriode.id, spgid)
                }
            }
            beregnUtbetaling(nyPeriode.somReferanse(), saksbehandler.bruker)
        }

        return nyPeriode
    }

    suspend fun finnPerioderForPerson(naturligIdent: NaturligIdent): List<Behandling> = db.nonTransactional { behandlingDao.finnBehandlingerForNaturligIdent(naturligIdent) }

    suspend fun sendTilBeslutning(
        periodeRef: BehandlingReferanse,
        individuellBegrunnelse: String?,
        saksbehandler: Bruker,
    ): Behandling =
        db.transactional {
            behandlingDao
                .let { dao ->
                    val periode = dao.hentPeriode(periodeRef, krav = saksbehandler.erSaksbehandlerPåSaken())

                    fun Behandling.harAlleredeBeslutter() = this.beslutterNavIdent != null
                    val nyStatus =
                        if (periode.harAlleredeBeslutter()) {
                            UNDER_BESLUTNING
                        } else {
                            TIL_BESLUTNING
                        }
                    periode.verifiserNyStatusGyldighet(nyStatus)
                    dao.endreStatusOgIndividuellBegrunnelse(periode, nyStatus = nyStatus, individuellBegrunnelse)
                    dao.reload(periode)
                }.also { oppdatertPeriode ->
                    behandlingEndringerDao.leggTilEndring(
                        oppdatertPeriode.endring(
                            endringType = SaksbehandlingsperiodeEndringType.SENDT_TIL_BESLUTNING,
                            saksbehandler = saksbehandler,
                        ),
                    )
                }
        }

    suspend fun revurderPeriode(
        periodeRef: BehandlingReferanse,
        saksbehandler: Bruker,
    ): Behandling =
        db.transactional {
            val forrigePeriode = behandlingDao.hentPeriode(periodeRef, null, måVæreUnderBehandling = false)
            if (forrigePeriode.status != GODKJENT) {
                throw InputValideringException("Kun godkjente perioder kan revurderes")
            }
            // TODO sjekke at ingen andre revurderer den? Eller bare stole på unique constraint i db?
            val nyPeriode =
                forrigePeriode.copy(
                    status = UNDER_BEHANDLING,
                    opprettet = OffsetDateTime.now(),
                    opprettetAvNavn = saksbehandler.navn,
                    opprettetAvNavIdent = saksbehandler.navIdent,
                    id = UUID.randomUUID(),
                    revurdererSaksbehandlingsperiodeId = forrigePeriode.id,
                )
            behandlingDao.opprettPeriode(nyPeriode)
            yrkesaktivitetDao.hentYrkesaktiviteterDbRecord(forrigePeriode).forEach { ya ->
                yrkesaktivitetDao.opprettYrkesaktivitet(
                    ya.copy(
                        id = UUID.randomUUID(),
                        saksbehandlingsperiodeId = nyPeriode.id,
                        opprettet = OffsetDateTime.now(),
                    ),
                )
            }

            tilkommenInntektDao.hentForBehandling(forrigePeriode.id).forEach { tilkommenInntektDbRecord ->
                tilkommenInntektDao.opprett(
                    tilkommenInntektDbRecord.copy(id = UUID.randomUUID(), behandlingId = nyPeriode.id),
                )
            }

            if (forrigePeriode.sykepengegrunnlagId != null) {
                sykepengegrunnlagDao.hentSykepengegrunnlag(forrigePeriode.sykepengegrunnlagId).let { spg ->
                    if (spg.opprettetForBehandling == forrigePeriode.id) {
                        val nyttSpg =
                            sykepengegrunnlagDao.lagreSykepengegrunnlag(
                                spg.sykepengegrunnlag,
                                saksbehandler,
                                nyPeriode.id,
                            )
                        behandlingDao.oppdaterSykepengegrunnlagId(
                            nyPeriode.id,
                            nyttSpg.id,
                        )
                    }
                }
            }

            behandlingEndringerDao.hentEndringerFor(forrigePeriode.id).forEach { e ->
                behandlingEndringerDao.leggTilEndring(
                    nyPeriode.endring(
                        endringType = e.endringType,
                        saksbehandler = saksbehandler,
                        status = nyPeriode.status,
                        beslutterNavIdent = nyPeriode.beslutterNavIdent,
                        endretTidspunkt = e.endretTidspunkt,
                        endringKommentar = e.endringKommentar,
                    ),
                )
            }
            behandlingEndringerDao.leggTilEndring(
                nyPeriode.endring(
                    endringType = REVURDERING_STARTET,
                    saksbehandler = saksbehandler,
                ),
            )

            vurdertVilkårDao.hentVilkårsvurderinger(forrigePeriode.id).forEach { v ->
                vurdertVilkårDao.leggTil(
                    nyPeriode,
                    Kode(v.kode),
                    v.vurdering,
                )
            }

            dokumentDao.hentDokumenterFor(forrigePeriode.id).forEach { dokument ->
                dokumentDao.opprettDokument(
                    dokument.copy(id = UUID.randomUUID(), opprettetForBehandling = nyPeriode.id),
                )
            }

            beregnSykepengegrunnlagOgUtbetaling(
                nyPeriode.somReferanse(),
                saksbehandler = saksbehandler,
            )

            behandlingDao.reload(nyPeriode)
        }

    suspend fun taTilBeslutning(
        periodeRef: BehandlingReferanse,
        saksbehandler: Bruker,
    ): Behandling =
        db.transactional {
            val periode = behandlingDao.hentPeriode(periodeRef, krav = null, måVæreUnderBehandling = false)
            // TODO: krevAtBrukerErBeslutter() ? (verifiseres dog allerede i RolleMatrise)
            val nyStatus = UNDER_BESLUTNING
            periode.verifiserNyStatusGyldighet(nyStatus)
            behandlingDao.endreStatusOgBeslutter(
                periode,
                nyStatus = nyStatus,
                beslutterNavIdent = saksbehandler.navIdent,
            )
            behandlingDao.reload(periode).also { oppdatertPeriode ->
                behandlingEndringerDao.leggTilEndring(
                    oppdatertPeriode.endring(
                        endringType = SaksbehandlingsperiodeEndringType.TATT_TIL_BESLUTNING,
                        saksbehandler = saksbehandler,
                    ),
                )
            }
        }

    suspend fun sendTilbakeFraBeslutning(
        periodeRef: BehandlingReferanse,
        saksbehandler: Bruker,
        kommentar: String,
    ): Behandling =
        db.transactional {
            val periode =
                behandlingDao.hentPeriode(
                    periodeRef,
                    krav = saksbehandler.erBeslutterPåSaken(),
                    måVæreUnderBehandling = false,
                )
            val nyStatus = UNDER_BEHANDLING
            periode.verifiserNyStatusGyldighet(nyStatus)
            behandlingDao.endreStatus(
                periode,
                nyStatus = nyStatus,
            )
            behandlingDao.reload(periode).also { oppdatertPeriode ->
                behandlingEndringerDao.leggTilEndring(
                    oppdatertPeriode.endring(
                        endringType = SaksbehandlingsperiodeEndringType.SENDT_I_RETUR,
                        saksbehandler = saksbehandler,
                        endringKommentar = kommentar,
                    ),
                )
            }
        }

    suspend fun godkjennPeriode(
        periodeRef: BehandlingReferanse,
        saksbehandler: Bruker,
    ): Behandling {
        return db.transactional {
            val periode =
                behandlingDao.hentPeriode(
                    periodeRef,
                    krav = saksbehandler.erBeslutterPåSaken(),
                    måVæreUnderBehandling = false,
                )
            val nyStatus = GODKJENT
            periode.verifiserNyStatusGyldighet(nyStatus)
            behandlingDao.endreStatusOgBeslutter(
                periode,
                nyStatus = nyStatus,
                beslutterNavIdent = saksbehandler.navIdent,
            )

            periode.sykepengegrunnlagId?.let {
                // Sjekk om perioden eier sykepengegrunnlaget, så skal det låses
                val sykepengegrunnlag = sykepengegrunnlagDao.hentSykepengegrunnlag(it)
                if (sykepengegrunnlag.opprettetForBehandling == periode.id) {
                    sykepengegrunnlagDao.settLåst(it)
                }
            }

            periode.revurdererSaksbehandlingsperiodeId?.let {
                behandlingDao.finnBehandling(it)?.let { revurdertPeriode ->
                    behandlingDao.endreStatus(revurdertPeriode, REVURDERT)
                    behandlingDao.oppdaterRevurdertAvBehandlingId(revurdertPeriode.id, periode.id)
                }
            }

            val oppdatertPeriode = behandlingDao.reload(periode)
            behandlingEndringerDao.leggTilEndring(
                oppdatertPeriode.endring(
                    endringType = SaksbehandlingsperiodeEndringType.GODKJENT,
                    saksbehandler = saksbehandler,
                ),
            )
            leggTilOutbox(periodeRef)
            return@transactional oppdatertPeriode
        }
    }

    suspend fun hentHistorikkFor(periodeRef: BehandlingReferanse): List<SaksbehandlingsperiodeEndring> =
        db.nonTransactional {
            val periode = behandlingDao.hentPeriode(periodeRef, krav = null, måVæreUnderBehandling = false)
            behandlingEndringerDao.hentEndringerFor(periode.id)
        }

    suspend fun oppdaterSkjæringstidspunkt(
        periodeRef: BehandlingReferanse,
        skjæringstidspunkt: LocalDate,
        saksbehandler: Bruker,
    ): Behandling =
        db.transactional {
            val periode =
                behandlingDao.hentPeriode(periodeRef, krav = saksbehandler.erSaksbehandlerPåSaken())
            behandlingDao.oppdaterSkjæringstidspunkt(periode.id, skjæringstidspunkt)
            behandlingDao.reload(periode).also { oppdatertPeriode ->
                behandlingEndringerDao.leggTilEndring(
                    oppdatertPeriode.endring(
                        endringType = SaksbehandlingsperiodeEndringType.OPPDATERT_SKJÆRINGSTIDSPUNKT,
                        saksbehandler = saksbehandler,
                    ),
                )
            }
        }
}

private fun Behandling.endring(
    endringType: SaksbehandlingsperiodeEndringType,
    saksbehandler: Bruker,
    status: BehandlingStatus = this.status,
    beslutterNavIdent: String? = this.beslutterNavIdent,
    endretTidspunkt: OffsetDateTime = OffsetDateTime.now(),
    endringKommentar: String? = null,
) = SaksbehandlingsperiodeEndring(
    saksbehandlingsperiodeId = this.id,
    status = status,
    beslutterNavIdent = beslutterNavIdent,
    endretTidspunkt = endretTidspunkt,
    endretAvNavIdent = saksbehandler.navIdent,
    endringType = endringType,
    endringKommentar = endringKommentar,
)

private fun Behandling.verifiserNyStatusGyldighet(nyStatus: BehandlingStatus) {
    if (!BehandlingStatus.erGyldigEndring(status to nyStatus)) {
        throw InputValideringException("Ugyldig statusendring: $status til $nyStatus")
    }
}

fun SykepengesoknadDTO.kategorisering(): YrkesaktivitetKategorisering {
    val soknad = this@kategorisering
    val orgnummer = soknad.arbeidsgiver?.orgnummer ?: "000000000" // Default orgnummer

    return when (soknad.arbeidssituasjon) {
        ArbeidssituasjonDTO.ARBEIDSTAKER -> {
            YrkesaktivitetKategorisering.Arbeidstaker(
                sykmeldt = true, // Anta at søknad betyr sykmeldt
                typeArbeidstaker =
                    TypeArbeidstaker.Ordinær(
                        orgnummer = orgnummer,
                    ),
            )
        }

        ArbeidssituasjonDTO.FRILANSER -> {
            YrkesaktivitetKategorisering.Frilanser(
                orgnummer = orgnummer,
                sykmeldt = true,
                forsikring = FrilanserForsikring.INGEN_FORSIKRING,
            )
        }

        ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE -> {
            YrkesaktivitetKategorisering.SelvstendigNæringsdrivende(
                sykmeldt = true,
                typeSelvstendigNæringsdrivende =
                    TypeSelvstendigNæringsdrivende.Ordinær(
                        forsikring = SelvstendigForsikring.INGEN_FORSIKRING,
                    ),
            )
        }

        ArbeidssituasjonDTO.FISKER -> {
            YrkesaktivitetKategorisering.SelvstendigNæringsdrivende(
                sykmeldt = true,
                typeSelvstendigNæringsdrivende = TypeSelvstendigNæringsdrivende.Fisker(),
            )
        }

        ArbeidssituasjonDTO.JORDBRUKER -> {
            YrkesaktivitetKategorisering.SelvstendigNæringsdrivende(
                sykmeldt = true,
                typeSelvstendigNæringsdrivende =
                    TypeSelvstendigNæringsdrivende.Jordbruker(
                        forsikring = SelvstendigForsikring.INGEN_FORSIKRING,
                    ),
            )
        }

        ArbeidssituasjonDTO.BARNEPASSER -> {
            YrkesaktivitetKategorisering.SelvstendigNæringsdrivende(
                sykmeldt = true,
                typeSelvstendigNæringsdrivende =
                    TypeSelvstendigNæringsdrivende.BarnepasserEgetHjem(
                        forsikring = SelvstendigForsikring.INGEN_FORSIKRING,
                    ),
            )
        }

        ArbeidssituasjonDTO.ARBEIDSLEDIG -> {
            YrkesaktivitetKategorisering.Arbeidsledig()
        }

        ArbeidssituasjonDTO.ANNET -> {
            YrkesaktivitetKategorisering.Inaktiv()
        }

        null -> {
            logg.warn("'null'-verdi for arbeidssituasjon for søknad med id={}", id)
            YrkesaktivitetKategorisering.Inaktiv()
        }
    }
}

fun lagYrkesaktiviteter(
    sykepengesoknader: Iterable<Dokument>,
    behandling: Behandling,
    tidligereYrkesaktiviteter: List<YrkesaktivitetDbRecord>,
): Pair<List<YrkesaktivitetDbRecord>, Map<UUID, UUID>> {
    val tidligereMap = tidligereYrkesaktiviteter.associateBy { it.kategorisering }
    val søknaderPerKategori = sykepengesoknader.groupBy { it.somSøknad().kategorisering() }

    val søknadKategorierMap = søknaderPerKategori

    val kategorier = (tidligereMap.keys + søknadKategorierMap.keys).toSet()

    val gammelTilNyIdMap = mutableMapOf<UUID, UUID>()

    val result =
        kategorier.mapNotNull { kategori ->
            søknadKategorierMap[kategori]?.let { søknader ->
                val sykdomstidlinje =
                    skapDagoversiktFraSoknader(
                        søknader.map { it.somSøknad() },
                        behandling.fom,
                        behandling.tom,
                    )
                val tidligere = tidligereMap[kategori]

                YrkesaktivitetDbRecord(
                    id = UUID.randomUUID(),
                    kategorisering = kategori,
                    kategoriseringGenerert = kategori,
                    dagoversikt = Dagoversikt(sykdomstidlinje, emptyList()),
                    dagoversiktGenerert = Dagoversikt(sykdomstidlinje, emptyList()),
                    saksbehandlingsperiodeId = behandling.id,
                    opprettet = OffsetDateTime.now(),
                    generertFraDokumenter = søknader.map { it.id },
                    refusjon = tidligere?.refusjon,
                    inntektData = tidligere?.inntektData,
                    inntektRequest = tidligere?.inntektRequest,
                )
            } ?: tidligereMap[kategori]?.let { tidligere ->
                val nyId = UUID.randomUUID()
                gammelTilNyIdMap[tidligere.id] = nyId
                tidligere.copy(
                    id = nyId,
                    dagoversikt = Dagoversikt(initialiserDager(behandling.fom, behandling.tom), emptyList()),
                    dagoversiktGenerert = null,
                    generertFraDokumenter = emptyList(),
                    saksbehandlingsperiodeId = behandling.id,
                    opprettet = OffsetDateTime.now(),
                )
            }
        }

    return result to gammelTilNyIdMap
}
