package no.nav.helse.bakrommet.behandling

import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.behandling.BehandlingStatus.*
import no.nav.helse.bakrommet.behandling.SaksbehandlingsperiodeEndringType.REVURDERING_STARTET
import no.nav.helse.bakrommet.behandling.beregning.Beregningsdaoer
import no.nav.helse.bakrommet.behandling.beregning.beregnSykepengegrunnlagOgUtbetaling
import no.nav.helse.bakrommet.behandling.beregning.beregnUtbetaling
import no.nav.helse.bakrommet.behandling.dagoversikt.initialiserDager
import no.nav.helse.bakrommet.behandling.dagoversikt.skapDagoversiktFraSoknader
import no.nav.helse.bakrommet.behandling.dokumenter.Dokument
import no.nav.helse.bakrommet.behandling.dokumenter.DokumentDao
import no.nav.helse.bakrommet.behandling.dokumenter.DokumentHenter
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetDbRecord
import no.nav.helse.bakrommet.domain.Bruker
import no.nav.helse.bakrommet.domain.person.NaturligIdent
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Behandling
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId
import no.nav.helse.bakrommet.domain.sykepenger.Dagoversikt
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.*
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.infrastruktur.db.Repositories
import no.nav.helse.bakrommet.kafka.SaksbehandlingsperiodeKafkaDtoDaoer
import no.nav.helse.bakrommet.kafka.leggTilOutbox
import no.nav.helse.bakrommet.logg
import no.nav.helse.flex.sykepengesoknad.kafka.ArbeidssituasjonDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

interface BehandlingServiceDaoer :
    SaksbehandlingsperiodeKafkaDtoDaoer,
    Repositories,
    Beregningsdaoer {
    val behandlingEndringerDao: BehandlingEndringerDao
    val dokumentDao: DokumentDao
}

data class BehandlingReferanse(
    val naturligIdent: NaturligIdent,
    val behandlingId: UUID,
)

fun BehandlingDbRecord.somReferanse() =
    BehandlingReferanse(
        naturligIdent = this.naturligIdent,
        behandlingId = this.id,
    )

class BehandlingService(
    private val db: DbDaoer<BehandlingServiceDaoer>,
    private val dokumentHenter: DokumentHenter,
) {
    suspend fun hentAlleSaksbehandlingsperioder() = db.nonTransactional { behandlingDao.hentAlleBehandlinger() }

    suspend fun opprettNyBehandling(
        naturligIdent: NaturligIdent,
        fom: LocalDate,
        tom: LocalDate,
        søknader: Set<UUID>,
        saksbehandler: BrukerOgToken,
        id: UUID = UUID.randomUUID(),
    ): BehandlingDbRecord {
        if (fom.isAfter(tom)) throw InputValideringException("Fom-dato kan ikke være etter tom-dato")
        var nyPeriode =
            BehandlingDbRecord(
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

        var tidligerePeriodeInntilNyPeriode: BehandlingDbRecord? = null

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
            val nyBehandlingId = BehandlingId(nyPeriode.id)
            behandlingEndringerDao.leggTilEndring(
                nyPeriode.endring(
                    endringType = SaksbehandlingsperiodeEndringType.STARTET,
                    saksbehandler = saksbehandler.bruker,
                ),
            )
            tidligerePeriodeInntilNyPeriode?.let { forrigePeriode ->
                vilkårsvurderingRepository
                    .hentAlle(BehandlingId(forrigePeriode.id))
                    .forEach { vurdertVilkår ->
                        vilkårsvurderingRepository.lagre(vurdertVilkår.kopierTil(nyBehandlingId))
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

            val (yrkesaktiviteter, _) =
                lagYrkesaktiviteter(
                    sykepengesoknader = søknader,
                    behandlingDbRecord = nyPeriode,
                    tidligereYrkesaktiviteter = tidligereYrkesaktiviteter,
                )
            yrkesaktiviteter.forEach { yrkesaktivitet ->
                // Konverter til sealed class og bruk den nye DAO-funksjonen
                yrkesaktivitetDao.opprettYrkesaktivitet(
                    id = yrkesaktivitet.id,
                    kategorisering = yrkesaktivitet.kategorisering,
                    dagoversikt = yrkesaktivitet.dagoversikt,
                    behandlingId = yrkesaktivitet.behandlingId,
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

    suspend fun finnPerioderForPerson(naturligIdent: NaturligIdent): List<BehandlingDbRecord> = db.nonTransactional { behandlingDao.finnBehandlingerForNaturligIdent(naturligIdent) }

    suspend fun sendTilBeslutning(
        periodeRef: BehandlingReferanse,
        individuellBegrunnelse: String?,
        saksbehandler: Bruker,
    ): BehandlingDbRecord =
        db.transactional {
            behandlingDao
                .let { dao ->
                    val periode = dao.hentPeriode(periodeRef, krav = saksbehandler.erSaksbehandlerPåSaken())

                    fun BehandlingDbRecord.harAlleredeBeslutter() = this.beslutterNavIdent != null
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
    ): BehandlingDbRecord =
        db.transactional {
            val behandlingId = BehandlingId(periodeRef.behandlingId)
            val forrigeBehandling = behandlingRepository.finn(behandlingId) ?: error("Forventer å finne en behandling")
            if (!forrigeBehandling.erGodkjent()) {
                throw InputValideringException("Kun godkjente perioder kan revurderes")
            }
            // TODO sjekke at ingen andre revurderer den? Eller bare stole på unique constraint i db?
            val nyBehandling = forrigeBehandling.revurderAv(navn = saksbehandler.navn, navIdent = saksbehandler.navIdent)
            behandlingRepository.lagre(forrigeBehandling)
            behandlingRepository.lagre(nyBehandling)

            val yrkesaktiviteter = yrkesaktivitetsperiodeRepository.finn(forrigeBehandling.id)
            yrkesaktiviteter
                .map { ya ->
                    ya.revurderUnder(behandlingId = nyBehandling.id)
                }.forEach { ny ->
                    yrkesaktivitetsperiodeRepository.lagre(ny)
                }

            tilkommenInntektRepository
                .finnFor(forrigeBehandling.id)
                .map {
                    it.revurderUnder(behandlingId = nyBehandling.id)
                }.forEach { ny ->
                    tilkommenInntektRepository.lagre(ny)
                }

            val sykepengegrunnlagId = forrigeBehandling.sykepengegrunnlagId
            if (sykepengegrunnlagId != null) {
                sykepengegrunnlagDao.hentSykepengegrunnlag(sykepengegrunnlagId).let { spg ->
                    if (spg.opprettetForBehandling == forrigeBehandling.id.value) {
                        val nyttSpg =
                            sykepengegrunnlagDao.lagreSykepengegrunnlag(
                                spg.sykepengegrunnlag,
                                saksbehandler,
                                nyBehandling.id.value,
                            )
                        behandlingDao.oppdaterSykepengegrunnlagId(
                            nyBehandling.id.value,
                            nyttSpg.id,
                        )
                    }
                }
            }

            behandlingEndringerDao.hentEndringerFor(forrigeBehandling.id.value).forEach { e ->
                behandlingEndringerDao.leggTilEndring(
                    nyBehandling.endring(
                        endringType = e.endringType,
                        saksbehandler = saksbehandler,
                        status = nyBehandling.status,
                        beslutterNavIdent = nyBehandling.beslutterNavIdent,
                        endretTidspunkt = e.endretTidspunkt,
                        endringKommentar = e.endringKommentar,
                    ),
                )
            }
            behandlingEndringerDao.leggTilEndring(
                nyBehandling.endring(
                    endringType = REVURDERING_STARTET,
                    saksbehandler = saksbehandler,
                ),
            )

            vilkårsvurderingRepository
                .hentAlle(forrigeBehandling.id)
                .forEach { vurdertVilkår ->
                    vilkårsvurderingRepository.lagre(vurdertVilkår.kopierTil(nyBehandling.id))
                }

            dokumentDao.hentDokumenterFor(forrigeBehandling.id.value).forEach { dokument ->
                dokumentDao.opprettDokument(
                    dokument.copy(id = UUID.randomUUID(), opprettetForBehandling = nyBehandling.id.value),
                )
            }

            beregnSykepengegrunnlagOgUtbetaling(
                BehandlingReferanse(nyBehandling.naturligIdent, nyBehandling.id.value),
                saksbehandler = saksbehandler,
            )

            behandlingDao.finnBehandling(nyBehandling.id.value)!!
        }

    suspend fun taTilBeslutning(
        periodeRef: BehandlingReferanse,
        saksbehandler: Bruker,
    ): BehandlingDbRecord =
        db.transactional {
            val periode = behandlingDao.hentPeriode(periodeRef, krav = null, måVæreUnderBehandling = false)
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
    ): BehandlingDbRecord =
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
    ): BehandlingDbRecord {
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
    ): BehandlingDbRecord =
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

private fun BehandlingDbRecord.endring(
    endringType: SaksbehandlingsperiodeEndringType,
    saksbehandler: Bruker,
    status: BehandlingStatus = this.status,
    beslutterNavIdent: String? = this.beslutterNavIdent,
    endretTidspunkt: OffsetDateTime = OffsetDateTime.now(),
    endringKommentar: String? = null,
) = SaksbehandlingsperiodeEndring(
    behandlingId = this.id,
    status = status,
    beslutterNavIdent = beslutterNavIdent,
    endretTidspunkt = endretTidspunkt,
    endretAvNavIdent = saksbehandler.navIdent,
    endringType = endringType,
    endringKommentar = endringKommentar,
)

private fun Behandling.endring(
    endringType: SaksbehandlingsperiodeEndringType,
    saksbehandler: Bruker,
    status: no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingStatus = this.status,
    beslutterNavIdent: String? = this.beslutterNavIdent,
    endretTidspunkt: OffsetDateTime = OffsetDateTime.now(),
    endringKommentar: String? = null,
) = SaksbehandlingsperiodeEndring(
    behandlingId = this.id.value,
    status =
        when (status) {
            no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingStatus.UNDER_BEHANDLING -> UNDER_BEHANDLING
            no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingStatus.TIL_BESLUTNING -> TIL_BESLUTNING
            no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingStatus.UNDER_BESLUTNING -> UNDER_BESLUTNING
            no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingStatus.GODKJENT -> GODKJENT
            no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingStatus.REVURDERT -> REVURDERT
        },
    beslutterNavIdent = beslutterNavIdent,
    endretTidspunkt = endretTidspunkt,
    endretAvNavIdent = saksbehandler.navIdent,
    endringType = endringType,
    endringKommentar = endringKommentar,
)

private fun BehandlingDbRecord.verifiserNyStatusGyldighet(nyStatus: BehandlingStatus) {
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

/**
 * Lager en matching-nøkkel fra kategorisering uten å inkludere sykmeldt-status.
 * Dette gjør at yrkesaktiviteter kan matche selv om sykmeldt-statusen er forskjellig.
 */
private fun YrkesaktivitetKategorisering.matchingNøkkel(): String =
    when (this) {
        is YrkesaktivitetKategorisering.Arbeidstaker -> {
            "ARBEIDSTAKER-${typeArbeidstaker.javaClass.simpleName}-${when (typeArbeidstaker) {
                is TypeArbeidstaker.Ordinær -> (typeArbeidstaker as TypeArbeidstaker.Ordinær).orgnummer
                is TypeArbeidstaker.Maritim -> (typeArbeidstaker as TypeArbeidstaker.Maritim).orgnummer
                is TypeArbeidstaker.Fisker -> (typeArbeidstaker as TypeArbeidstaker.Fisker).orgnummer
                is TypeArbeidstaker.PrivatArbeidsgiver -> (typeArbeidstaker as TypeArbeidstaker.PrivatArbeidsgiver).arbeidsgiverFnr
                else -> ""
            }}"
        }

        is YrkesaktivitetKategorisering.Frilanser -> {
            "FRILANSER-$orgnummer-${forsikring.name}"
        }

        is YrkesaktivitetKategorisering.SelvstendigNæringsdrivende -> {
            "SELVSTENDIG_NÆRINGSDRIVENDE-${typeSelvstendigNæringsdrivende.javaClass.simpleName}-${typeSelvstendigNæringsdrivende.forsikring.name}"
        }

        is YrkesaktivitetKategorisering.Inaktiv -> {
            "INAKTIV"
        }

        is YrkesaktivitetKategorisering.Arbeidsledig -> {
            "ARBEIDSLEDIG"
        }
    }

/**
 * Setter sykmeldt-status på en kategorisering.
 */
private fun YrkesaktivitetKategorisering.medSykmeldt(sykmeldt: Boolean): YrkesaktivitetKategorisering =
    when (this) {
        is YrkesaktivitetKategorisering.Arbeidstaker -> copy(sykmeldt = sykmeldt)
        is YrkesaktivitetKategorisering.Frilanser -> copy(sykmeldt = sykmeldt)
        is YrkesaktivitetKategorisering.SelvstendigNæringsdrivende -> copy(sykmeldt = sykmeldt)
        is YrkesaktivitetKategorisering.Inaktiv -> copy(sykmeldt = sykmeldt)
        is YrkesaktivitetKategorisering.Arbeidsledig -> copy(sykmeldt = sykmeldt)
    }

fun lagYrkesaktiviteter(
    sykepengesoknader: Iterable<Dokument>,
    behandlingDbRecord: BehandlingDbRecord,
    tidligereYrkesaktiviteter: List<YrkesaktivitetDbRecord>,
): Pair<List<YrkesaktivitetDbRecord>, Map<UUID, UUID>> {
    // Bruk matching-nøkkel uten sykmeldt for å matche yrkesaktiviteter
    val tidligereMap = tidligereYrkesaktiviteter.associateBy { it.kategorisering.matchingNøkkel() }
    val søknaderPerKategori = sykepengesoknader.groupBy { it.somSøknad().kategorisering().matchingNøkkel() }

    val søknadKategorierMap = søknaderPerKategori

    val kategorier = (tidligereMap.keys + søknadKategorierMap.keys).toSet()

    val gammelTilNyIdMap = mutableMapOf<UUID, UUID>()

    val result =
        kategorier.mapNotNull { matchingNøkkel ->
            søknadKategorierMap[matchingNøkkel]?.let { søknader ->
                val søknadKategorisering = søknader.first().somSøknad().kategorisering()
                val tidligere = tidligereMap[matchingNøkkel]

                // Sett sykmeldt = true når det finnes søknader
                val kategoriseringMedSykmeldt = søknadKategorisering.medSykmeldt(true)

                val sykdomstidlinje =
                    skapDagoversiktFraSoknader(
                        søknader.map { it.somSøknad() },
                        behandlingDbRecord.fom,
                        behandlingDbRecord.tom,
                    )

                YrkesaktivitetDbRecord(
                    id = UUID.randomUUID(),
                    kategorisering = kategoriseringMedSykmeldt,
                    kategoriseringGenerert = kategoriseringMedSykmeldt,
                    dagoversikt = Dagoversikt(sykdomstidlinje, emptyList()),
                    dagoversiktGenerert = Dagoversikt(sykdomstidlinje, emptyList()),
                    behandlingId = behandlingDbRecord.id,
                    opprettet = OffsetDateTime.now(),
                    generertFraDokumenter = søknader.map { it.id },
                    refusjon = tidligere?.refusjon,
                    inntektData = tidligere?.inntektData,
                    inntektRequest = tidligere?.inntektRequest,
                )
            } ?: tidligereMap[matchingNøkkel]?.let { tidligere ->
                val nyId = UUID.randomUUID()
                gammelTilNyIdMap[tidligere.id] = nyId
                // Behold sykmeldt-status fra tidligere yrkesaktivitet når det ikke finnes søknader
                tidligere.copy(
                    id = nyId,
                    dagoversikt = Dagoversikt(initialiserDager(behandlingDbRecord.fom, behandlingDbRecord.tom), emptyList()),
                    dagoversiktGenerert = null,
                    generertFraDokumenter = emptyList(),
                    behandlingId = behandlingDbRecord.id,
                    opprettet = OffsetDateTime.now(),
                )
            }
        }

    return result to gammelTilNyIdMap
}
