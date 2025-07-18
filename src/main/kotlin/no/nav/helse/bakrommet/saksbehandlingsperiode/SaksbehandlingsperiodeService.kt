package no.nav.helse.bakrommet.saksbehandlingsperiode

import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.infrastruktur.db.TransactionalSessionFactory
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.person.SpilleromPersonId
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.skapDagoversiktFraSoknader
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.Dokument
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.DokumentDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.DokumentHenter
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold.Inntektsforhold
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold.InntektsforholdDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold.Kategorisering
import no.nav.helse.bakrommet.util.logg
import no.nav.helse.bakrommet.util.objectMapper
import no.nav.helse.bakrommet.util.tilJsonNode
import no.nav.helse.flex.sykepengesoknad.kafka.ArbeidssituasjonDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

interface SaksbehandlingsperiodeServiceDaoer {
    val saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao
    val saksbehandlingsperiodeEndringerDao: SaksbehandlingsperiodeEndringerDao
    val personDao: PersonDao
    val dokumentDao: DokumentDao
    val inntektsforholdDao: InntektsforholdDao
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

    fun hentPeriode(ref: SaksbehandlingsperiodeReferanse) = db.nonTransactional { saksbehandlingsperiodeDao.hentPeriode(ref, krav = null) }

    suspend fun opprettNySaksbehandlingsperiode(
        spilleromPersonId: SpilleromPersonId,
        fom: LocalDate,
        tom: LocalDate,
        søknader: Set<UUID>,
        saksbehandler: BrukerOgToken,
    ): Saksbehandlingsperiode {
        if (fom.isAfter(tom)) throw InputValideringException("Fom-dato kan ikke være etter tom-dato")
        val nyPeriode =
            Saksbehandlingsperiode(
                id = UUID.randomUUID(),
                spilleromPersonId = spilleromPersonId.personId,
                opprettet = OffsetDateTime.now(),
                opprettetAvNavIdent = saksbehandler.bruker.navIdent,
                opprettetAvNavn = saksbehandler.bruker.navn,
                fom = fom,
                tom = tom,
            )
        db.transactional {
            saksbehandlingsperiodeDao.opprettPeriode(nyPeriode)
            saksbehandlingsperiodeEndringerDao.leggTilEndring(
                nyPeriode.endring(
                    endringType = SaksbehandlingsperiodeEndringType.STARTET,
                    saksbehandler = saksbehandler.bruker,
                ),
            )
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
            lagInntektsforholdFraSøknader(søknader, nyPeriode)
                .forEach(inntektsforholdDao::opprettInntektsforhold)
        }
        return nyPeriode
    }

    fun finnPerioderForPerson(spilleromPersonId: SpilleromPersonId): List<Saksbehandlingsperiode> {
        return db.nonTransactional { saksbehandlingsperiodeDao.finnPerioderForPerson(spilleromPersonId.personId) }
    }

    fun sendTilBeslutning(
        periodeRef: SaksbehandlingsperiodeReferanse,
        saksbehandler: Bruker,
    ): Saksbehandlingsperiode {
        return db.transactional {
            saksbehandlingsperiodeDao.let { dao ->
                val periode = dao.hentPeriode(periodeRef, krav = saksbehandler.erSaksbehandlerPåSaken())

                fun Saksbehandlingsperiode.harAlleredeBeslutter() = this.beslutterNavIdent != null
                val nyStatus =
                    if (periode.harAlleredeBeslutter()) {
                        SaksbehandlingsperiodeStatus.UNDER_BESLUTNING
                    } else {
                        SaksbehandlingsperiodeStatus.TIL_BESLUTNING
                    }
                periode.verifiserNyStatusGyldighet(nyStatus)
                dao.endreStatus(periode, nyStatus = nyStatus)
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
    }

    fun taTilBeslutning(
        periodeRef: SaksbehandlingsperiodeReferanse,
        saksbehandler: Bruker,
    ): Saksbehandlingsperiode {
        return db.transactional {
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
    }

    fun sendTilbakeFraBeslutning(
        periodeRef: SaksbehandlingsperiodeReferanse,
        saksbehandler: Bruker,
        kommentar: String,
    ): Saksbehandlingsperiode {
        return db.transactional {
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
            saksbehandlingsperiodeDao.reload(periode).also { oppdatertPeriode ->
                saksbehandlingsperiodeEndringerDao.leggTilEndring(
                    oppdatertPeriode.endring(
                        endringType = SaksbehandlingsperiodeEndringType.GODKJENT,
                        saksbehandler = saksbehandler,
                    ),
                )
            }
        }
    }

    fun hentHistorikkFor(periodeRef: SaksbehandlingsperiodeReferanse): List<SaksbehandlingsperiodeEndring> =
        db.nonTransactional {
            val periode = saksbehandlingsperiodeDao.hentPeriode(periodeRef, krav = null)
            saksbehandlingsperiodeEndringerDao.hentEndringerFor(periode.id)
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

fun SykepengesoknadDTO.kategorisering(): Kategorisering {
    return objectMapper.createObjectNode().apply {
        val soknad = this@kategorisering
        put("INNTEKTSKATEGORI", soknad.bestemInntektskategori())
        val orgnummer = soknad.arbeidsgiver?.orgnummer
        if (orgnummer != null) {
            put("ORGNUMMER", orgnummer)
        }
    }
}

fun lagInntektsforholdFraSøknader(
    sykepengesoknader: Iterable<Dokument>,
    saksbehandlingsperiode: Saksbehandlingsperiode,
): List<Inntektsforhold> {
    val kategorierOgSøknader =
        sykepengesoknader
            .groupBy { dokument -> dokument.somSøknad().kategorisering() }
    return kategorierOgSøknader.map { (kategorisering, dok) ->
        val dagoversikt = skapDagoversiktFraSoknader(dok.map { it.somSøknad() }, saksbehandlingsperiode.fom, saksbehandlingsperiode.tom)
        Inntektsforhold(
            id = UUID.randomUUID(),
            kategorisering = kategorisering,
            kategoriseringGenerert = kategorisering,
            dagoversikt = dagoversikt.tilJsonNode(),
            dagoversiktGenerert = dagoversikt.tilJsonNode(),
            saksbehandlingsperiodeId = saksbehandlingsperiode.id,
            opprettet = OffsetDateTime.now(),
            generertFraDokumenter = dok.map { it.id },
        )
    }
}

private fun SykepengesoknadDTO.bestemInntektskategori() =
    when (arbeidssituasjon) {
        ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE -> InntektsforholdType.SELVSTENDIG_NÆRINGSDRIVENDE
        ArbeidssituasjonDTO.FISKER -> InntektsforholdType.SELVSTENDIG_NÆRINGSDRIVENDE
        ArbeidssituasjonDTO.JORDBRUKER -> InntektsforholdType.SELVSTENDIG_NÆRINGSDRIVENDE
        ArbeidssituasjonDTO.FRILANSER -> InntektsforholdType.FRILANSER
        ArbeidssituasjonDTO.ARBEIDSTAKER -> InntektsforholdType.ARBEIDSTAKER
        ArbeidssituasjonDTO.ARBEIDSLEDIG -> InntektsforholdType.INAKTIV
        ArbeidssituasjonDTO.ANNET -> InntektsforholdType.ANNET
        null -> {
            logg.warn("'null'-verdi for arbeidssituasjon for søknad med id={}", id)
            "IKKE SATT"
        }
    }.toString()

// kopiert fra frontend:
private enum class InntektsforholdType {
    ARBEIDSTAKER,
    FRILANSER,
    SELVSTENDIG_NÆRINGSDRIVENDE,
    INAKTIV,
    ANNET,
}
