package no.nav.helse.bakrommet.saksbehandlingsperiode

import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.infrastruktur.db.TransactionalSessionFactory
import no.nav.helse.bakrommet.kafka.SaksbehandlingsperiodeKafkaDtoDaoer
import no.nav.helse.bakrommet.kafka.leggTilOutbox
import no.nav.helse.bakrommet.person.SpilleromPersonId
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.initialiserDager
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.skapDagoversiktFraSoknader
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.Dokument
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.DokumentDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.DokumentHenter
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.FrilanserForsikring
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.SelvstendigForsikring
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.TypeArbeidstaker
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.TypeSelvstendigNæringsdrivende
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.VariantAvInaktiv
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetDbRecord
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetKategoriseringMapper
import no.nav.helse.bakrommet.util.logg
import no.nav.helse.flex.sykepengesoknad.kafka.ArbeidssituasjonDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.*

interface SaksbehandlingsperiodeServiceDaoer : SaksbehandlingsperiodeKafkaDtoDaoer {
    val saksbehandlingsperiodeEndringerDao: SaksbehandlingsperiodeEndringerDao
    val dokumentDao: DokumentDao
}

data class SaksbehandlingsperiodeReferanse(
    val spilleromPersonId: SpilleromPersonId,
    val periodeUUID: UUID,
)

fun Saksbehandlingsperiode.somReferanse() =
    SaksbehandlingsperiodeReferanse(
        spilleromPersonId = SpilleromPersonId(this.spilleromPersonId),
        periodeUUID = this.id,
    )

class SaksbehandlingsperiodeService(
    daoer: SaksbehandlingsperiodeServiceDaoer,
    sessionFactory: TransactionalSessionFactory<SaksbehandlingsperiodeServiceDaoer>,
    private val dokumentHenter: DokumentHenter,
) {
    private val db = DbDaoer(daoer, sessionFactory)

    fun hentAlleSaksbehandlingsperioder() = db.nonTransactional { saksbehandlingsperiodeDao.hentAlleSaksbehandlingsperioder() }

    fun hentPeriode(ref: SaksbehandlingsperiodeReferanse) = db.nonTransactional { saksbehandlingsperiodeDao.hentPeriode(ref, krav = null) }

    suspend fun opprettNySaksbehandlingsperiode(
        spilleromPersonId: SpilleromPersonId,
        fom: LocalDate,
        tom: LocalDate,
        søknader: Set<UUID>,
        saksbehandler: BrukerOgToken,
    ): Saksbehandlingsperiode {
        if (fom.isAfter(tom)) throw InputValideringException("Fom-dato kan ikke være etter tom-dato")
        var nyPeriode =
            Saksbehandlingsperiode(
                id = UUID.randomUUID(),
                spilleromPersonId = spilleromPersonId.personId,
                opprettet = OffsetDateTime.now(),
                opprettetAvNavIdent = saksbehandler.bruker.navIdent,
                opprettetAvNavn = saksbehandler.bruker.navn,
                fom = fom,
                tom = tom,
                skjæringstidspunkt = fom,
            )

        var tidligerePeriodeInntilNyPeriode: Saksbehandlingsperiode? = null

        db.transactional {
            val perioder = saksbehandlingsperiodeDao.finnPerioderForPerson(spilleromPersonId.personId)

            if (perioder.any { it.fom <= tom && it.tom >= fom }) {
                throw InputValideringException("Angitte datoer overlapper med en eksisterende periode")
            }

            tidligerePeriodeInntilNyPeriode = perioder.find { it.tom.plusDays(1).isEqual(fom) }

            tidligerePeriodeInntilNyPeriode?.let {
                nyPeriode =
                    nyPeriode.copy(
                        skjæringstidspunkt = it.skjæringstidspunkt ?: fom,
                    )
            }

            saksbehandlingsperiodeDao.opprettPeriode(nyPeriode)
            saksbehandlingsperiodeEndringerDao.leggTilEndring(
                nyPeriode.endring(
                    endringType = SaksbehandlingsperiodeEndringType.STARTET,
                    saksbehandler = saksbehandler.bruker,
                ),
            )
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

        db.nonTransactional {
            val tidligereYrkesaktiviteter =
                tidligerePeriodeInntilNyPeriode
                    ?.let { yrkesaktivitetDao.hentYrkesaktiviteterDbRecord(it) }
                    ?: emptyList()

            val (yrkesaktiviteter, gammelTilNyIdMap) =
                lagYrkesaktiviteter(
                    sykepengesoknader = søknader,
                    saksbehandlingsperiode = nyPeriode,
                    tidligereYrkesaktiviteter = tidligereYrkesaktiviteter,
                )
            yrkesaktiviteter.forEach { yrkesaktivitet ->
                // Konverter til sealed class og bruk den nye DAO-funksjonen
                val kategorisering = YrkesaktivitetKategoriseringMapper.fromMap(yrkesaktivitet.kategorisering)
                yrkesaktivitetDao.opprettYrkesaktivitet(
                    id = yrkesaktivitet.id,
                    kategorisering = kategorisering,
                    dagoversikt = yrkesaktivitet.dagoversikt,
                    saksbehandlingsperiodeId = yrkesaktivitet.saksbehandlingsperiodeId,
                    opprettet = yrkesaktivitet.opprettet,
                    generertFraDokumenter = yrkesaktivitet.generertFraDokumenter,
                    perioder = yrkesaktivitet.perioder,
                )
            }

            tidligerePeriodeInntilNyPeriode?.let {
                sykepengegrunnlagDaoOld.hentSykepengegrunnlag(it.id)?.let { grunnlag ->
                    sykepengegrunnlagDaoOld.settSykepengegrunnlag(
                        saksbehandlingsperiodeId = nyPeriode.id,
                        beregning =
                            grunnlag.copy(
                                id = UUID.randomUUID(),
                                saksbehandlingsperiodeId = nyPeriode.id,
                                opprettet = LocalDateTime.now().toString(),
                                opprettetAv = saksbehandler.bruker.navIdent,
                                sistOppdatert = LocalDateTime.now().toString(),
                                inntekter =
                                    grunnlag.inntekter.map { inntekt ->
                                        val nyId = gammelTilNyIdMap[inntekt.yrkesaktivitetId] ?: UUID.randomUUID()
                                        inntekt.copy(yrkesaktivitetId = nyId)
                                    },
                            ),
                        saksbehandler = saksbehandler.bruker,
                    )
                }
            }
        }

        return nyPeriode
    }

    fun finnPerioderForPerson(spilleromPersonId: SpilleromPersonId): List<Saksbehandlingsperiode> = db.nonTransactional { saksbehandlingsperiodeDao.finnPerioderForPerson(spilleromPersonId.personId) }

    fun sendTilBeslutning(
        periodeRef: SaksbehandlingsperiodeReferanse,
        individuellBegrunnelse: String?,
        saksbehandler: Bruker,
    ): Saksbehandlingsperiode =
        db.transactional {
            saksbehandlingsperiodeDao
                .let { dao ->
                    val periode = dao.hentPeriode(periodeRef, krav = saksbehandler.erSaksbehandlerPåSaken())

                    fun Saksbehandlingsperiode.harAlleredeBeslutter() = this.beslutterNavIdent != null
                    val nyStatus =
                        if (periode.harAlleredeBeslutter()) {
                            SaksbehandlingsperiodeStatus.UNDER_BESLUTNING
                        } else {
                            SaksbehandlingsperiodeStatus.TIL_BESLUTNING
                        }
                    periode.verifiserNyStatusGyldighet(nyStatus)
                    dao.endreStatusOgIndividuellBegrunnelse(periode, nyStatus = nyStatus, individuellBegrunnelse)
                    dao.reload(periode)
                }.also { oppdatertPeriode ->
                    saksbehandlingsperiodeEndringerDao.leggTilEndring(
                        oppdatertPeriode.endring(
                            endringType = SaksbehandlingsperiodeEndringType.SENDT_TIL_BESLUTNING,
                            saksbehandler = saksbehandler,
                        ),
                    )
                }
        }

    fun taTilBeslutning(
        periodeRef: SaksbehandlingsperiodeReferanse,
        saksbehandler: Bruker,
    ): Saksbehandlingsperiode =
        db.transactional {
            val periode = saksbehandlingsperiodeDao.hentPeriode(periodeRef, krav = null)
            // TODO: krevAtBrukerErBeslutter() ? (verifiseres dog allerede i RolleMatrise)
            val nyStatus = SaksbehandlingsperiodeStatus.UNDER_BESLUTNING
            periode.verifiserNyStatusGyldighet(nyStatus)
            saksbehandlingsperiodeDao.endreStatusOgBeslutter(
                periode,
                nyStatus = nyStatus,
                beslutterNavIdent = saksbehandler.navIdent,
            )
            saksbehandlingsperiodeDao.reload(periode).also { oppdatertPeriode ->
                saksbehandlingsperiodeEndringerDao.leggTilEndring(
                    oppdatertPeriode.endring(
                        endringType = SaksbehandlingsperiodeEndringType.TATT_TIL_BESLUTNING,
                        saksbehandler = saksbehandler,
                    ),
                )
            }
        }

    fun sendTilbakeFraBeslutning(
        periodeRef: SaksbehandlingsperiodeReferanse,
        saksbehandler: Bruker,
        kommentar: String,
    ): Saksbehandlingsperiode =
        db.transactional {
            val periode = saksbehandlingsperiodeDao.hentPeriode(periodeRef, krav = saksbehandler.erBeslutterPåSaken())
            val nyStatus = SaksbehandlingsperiodeStatus.UNDER_BEHANDLING
            periode.verifiserNyStatusGyldighet(nyStatus)
            saksbehandlingsperiodeDao.endreStatus(
                periode,
                nyStatus = nyStatus,
            )
            saksbehandlingsperiodeDao.reload(periode).also { oppdatertPeriode ->
                saksbehandlingsperiodeEndringerDao.leggTilEndring(
                    oppdatertPeriode.endring(
                        endringType = SaksbehandlingsperiodeEndringType.SENDT_I_RETUR,
                        saksbehandler = saksbehandler,
                        endringKommentar = kommentar,
                    ),
                )
            }
        }

    fun godkjennPeriode(
        periodeRef: SaksbehandlingsperiodeReferanse,
        saksbehandler: Bruker,
    ): Saksbehandlingsperiode {
        return db.transactional {
            val periode = saksbehandlingsperiodeDao.hentPeriode(periodeRef, krav = saksbehandler.erBeslutterPåSaken())
            val nyStatus = SaksbehandlingsperiodeStatus.GODKJENT
            periode.verifiserNyStatusGyldighet(nyStatus)
            saksbehandlingsperiodeDao.endreStatusOgBeslutter(
                periode,
                nyStatus = nyStatus,
                beslutterNavIdent = saksbehandler.navIdent,
            )
            val oppdatertPeriode = saksbehandlingsperiodeDao.reload(periode)
            saksbehandlingsperiodeEndringerDao.leggTilEndring(
                oppdatertPeriode.endring(
                    endringType = SaksbehandlingsperiodeEndringType.GODKJENT,
                    saksbehandler = saksbehandler,
                ),
            )
            leggTilOutbox(periodeRef)
            return@transactional oppdatertPeriode
        }
    }

    fun hentHistorikkFor(periodeRef: SaksbehandlingsperiodeReferanse): List<SaksbehandlingsperiodeEndring> =
        db.nonTransactional {
            val periode = saksbehandlingsperiodeDao.hentPeriode(periodeRef, krav = null)
            saksbehandlingsperiodeEndringerDao.hentEndringerFor(periode.id)
        }

    fun oppdaterSkjæringstidspunkt(
        periodeRef: SaksbehandlingsperiodeReferanse,
        skjæringstidspunkt: LocalDate?,
        saksbehandler: Bruker,
    ): Saksbehandlingsperiode =
        db.transactional {
            val periode =
                saksbehandlingsperiodeDao.hentPeriode(periodeRef, krav = saksbehandler.erSaksbehandlerPåSaken())
            saksbehandlingsperiodeDao.oppdaterSkjæringstidspunkt(periode.id, skjæringstidspunkt)
            saksbehandlingsperiodeDao.reload(periode).also { oppdatertPeriode ->
                saksbehandlingsperiodeEndringerDao.leggTilEndring(
                    oppdatertPeriode.endring(
                        endringType = SaksbehandlingsperiodeEndringType.OPPDATERT_SKJÆRINGSTIDSPUNKT,
                        saksbehandler = saksbehandler,
                    ),
                )
            }
        }
}

private fun Saksbehandlingsperiode.endring(
    endringType: SaksbehandlingsperiodeEndringType,
    saksbehandler: Bruker,
    status: SaksbehandlingsperiodeStatus = this.status,
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

private fun Saksbehandlingsperiode.verifiserNyStatusGyldighet(nyStatus: SaksbehandlingsperiodeStatus) {
    if (!SaksbehandlingsperiodeStatus.erGyldigEndring(status to nyStatus)) {
        throw InputValideringException("Ugyldig statusendring: $status til $nyStatus")
    }
}

fun SykepengesoknadDTO.kategorisering(): YrkesaktivitetKategorisering {
    val soknad = this@kategorisering
    val orgnummer = soknad.arbeidsgiver?.orgnummer ?: "000000000" // Default orgnummer

    return when (soknad.arbeidssituasjon) {
        ArbeidssituasjonDTO.ARBEIDSTAKER -> {
            YrkesaktivitetKategorisering.Arbeidstaker(
                orgnummer = orgnummer,
                sykmeldt = true, // Anta at søknad betyr sykmeldt
                typeArbeidstaker = TypeArbeidstaker.ORDINÆRT_ARBEIDSFORHOLD,
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
                type =
                    TypeSelvstendigNæringsdrivende.Ordinær(
                        forsikring = SelvstendigForsikring.INGEN_FORSIKRING,
                    ),
            )
        }
        ArbeidssituasjonDTO.FISKER -> {
            YrkesaktivitetKategorisering.SelvstendigNæringsdrivende(
                sykmeldt = true,
                type = TypeSelvstendigNæringsdrivende.Fisker(),
            )
        }
        ArbeidssituasjonDTO.JORDBRUKER -> {
            YrkesaktivitetKategorisering.SelvstendigNæringsdrivende(
                sykmeldt = true,
                type =
                    TypeSelvstendigNæringsdrivende.Jordbruker(
                        forsikring = SelvstendigForsikring.INGEN_FORSIKRING,
                    ),
            )
        }
        ArbeidssituasjonDTO.BARNEPASSER -> {
            YrkesaktivitetKategorisering.SelvstendigNæringsdrivende(
                sykmeldt = true,
                type =
                    TypeSelvstendigNæringsdrivende.BarnepasserEgetHjem(
                        forsikring = SelvstendigForsikring.INGEN_FORSIKRING,
                    ),
            )
        }
        ArbeidssituasjonDTO.ARBEIDSLEDIG -> {
            YrkesaktivitetKategorisering.Arbeidsledig()
        }
        ArbeidssituasjonDTO.ANNET -> {
            YrkesaktivitetKategorisering.Inaktiv(
                variant = VariantAvInaktiv.INAKTIV_VARIANT_A,
            )
        }
        null -> {
            logg.warn("'null'-verdi for arbeidssituasjon for søknad med id={}", id)
            YrkesaktivitetKategorisering.Inaktiv(
                variant = VariantAvInaktiv.INAKTIV_VARIANT_A,
            )
        }
    }
}

fun lagYrkesaktivitetFraSøknader(
    sykepengesoknader: Iterable<Dokument>,
    saksbehandlingsperiode: Saksbehandlingsperiode,
): List<YrkesaktivitetDbRecord> {
    val kategorierOgSøknader =
        sykepengesoknader
            .groupBy { dokument -> dokument.somSøknad().kategorisering() }
    return kategorierOgSøknader.map { (kategorisering, dok) ->
        val dagoversikt =
            skapDagoversiktFraSoknader(
                dok.map { it.somSøknad() },
                saksbehandlingsperiode.fom,
                saksbehandlingsperiode.tom,
            )
        YrkesaktivitetDbRecord(
            id = UUID.randomUUID(),
            kategorisering = YrkesaktivitetKategoriseringMapper.toMap(kategorisering),
            kategoriseringGenerert = YrkesaktivitetKategoriseringMapper.toMap(kategorisering),
            dagoversikt = dagoversikt,
            dagoversiktGenerert = dagoversikt,
            saksbehandlingsperiodeId = saksbehandlingsperiode.id,
            opprettet = OffsetDateTime.now(),
            generertFraDokumenter = dok.map { it.id },
        )
    }
}

fun lagYrkesaktiviteter(
    sykepengesoknader: Iterable<Dokument>,
    saksbehandlingsperiode: Saksbehandlingsperiode,
    tidligereYrkesaktiviteter: List<YrkesaktivitetDbRecord>,
): Pair<List<YrkesaktivitetDbRecord>, Map<UUID, UUID>> {
    val tidligereMap = tidligereYrkesaktiviteter.associateBy { it.kategorisering }
    val søknaderPerKategori = sykepengesoknader.groupBy { it.somSøknad().kategorisering() }

    // Konverter søknad-kategorier til Map for sammenligning
    val søknadKategorierMap =
        søknaderPerKategori.mapKeys { (kategori, _) ->
            YrkesaktivitetKategoriseringMapper.toMap(kategori)
        }

    val kategorier = tidligereMap.keys + søknadKategorierMap.keys

    val gammelTilNyIdMap = mutableMapOf<UUID, UUID>()

    val result =
        kategorier.mapNotNull { kategoriMap ->
            søknadKategorierMap[kategoriMap]?.let { søknader ->
                val dagoversikt =
                    skapDagoversiktFraSoknader(
                        søknader.map { it.somSøknad() },
                        saksbehandlingsperiode.fom,
                        saksbehandlingsperiode.tom,
                    )
                YrkesaktivitetDbRecord(
                    id = UUID.randomUUID(),
                    kategorisering = kategoriMap,
                    kategoriseringGenerert = kategoriMap,
                    dagoversikt = dagoversikt,
                    dagoversiktGenerert = dagoversikt,
                    saksbehandlingsperiodeId = saksbehandlingsperiode.id,
                    opprettet = OffsetDateTime.now(),
                    generertFraDokumenter = søknader.map { it.id },
                )
            } ?: tidligereMap[kategoriMap]?.let { tidligere ->
                val nyId = UUID.randomUUID()
                gammelTilNyIdMap[tidligere.id] = nyId
                tidligere.copy(
                    id = nyId,
                    dagoversikt = initialiserDager(saksbehandlingsperiode.fom, saksbehandlingsperiode.tom),
                    dagoversiktGenerert = null,
                    generertFraDokumenter = emptyList(),
                    saksbehandlingsperiodeId = saksbehandlingsperiode.id,
                    opprettet = OffsetDateTime.now(),
                )
            }
        }

    return result to gammelTilNyIdMap
}
