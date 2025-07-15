package no.nav.helse.bakrommet.saksbehandlingsperiode

import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.errorhandling.ForbiddenException
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.errorhandling.SaksbehandlingsperiodeIkkeFunnetException
import no.nav.helse.bakrommet.infrastruktur.db.TransactionalSessionFactory
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.person.SpilleromPersonId
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold.InntektsforholdDao
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

interface SaksbehandlingsperiodeServiceDaoer {
    val saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao
    val personDao: PersonDao
    val dokumentDao: DokumentDao
    val inntektsforholdDao: InntektsforholdDao
}

data class SaksbehandlingsperiodeReferanse(
    val spilleromPersonId: SpilleromPersonId,
    val periodeUUID: UUID,
)

class SaksbehandlingsperiodeService(
    private val daoer: SaksbehandlingsperiodeServiceDaoer,
    private val sessionFactory: TransactionalSessionFactory<SaksbehandlingsperiodeServiceDaoer>,
    private val dokumentHenter: DokumentHenter,
) {
    private fun Saksbehandlingsperiode.reload() = daoer.saksbehandlingsperiodeDao.finnSaksbehandlingsperiode(id)!!

    fun hentPeriode(ref: SaksbehandlingsperiodeReferanse): Saksbehandlingsperiode {
        val periode =
            daoer.saksbehandlingsperiodeDao.finnSaksbehandlingsperiode(ref.periodeUUID)
                ?: throw SaksbehandlingsperiodeIkkeFunnetException()
        if (periode.spilleromPersonId != ref.spilleromPersonId.personId) {
            throw InputValideringException("Ugyldig saksbehandlingsperiode")
        }
        return periode
    }

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
        daoer.saksbehandlingsperiodeDao.opprettPeriode(nyPeriode)
        val søknader =
            if (søknader.isNotEmpty()) {
                dokumentHenter.hentOgLagreSøknader(
                    nyPeriode.id,
                    søknader.toList(),
                    saksbehandler.token,
                )
            } else {
                emptyList()
            }
        lagInntektsforholdFraSøknader(søknader, nyPeriode)
            .forEach(daoer.inntektsforholdDao::opprettInntektsforhold)
        return nyPeriode
    }

    fun finnPerioderForPerson(spilleromPersonId: SpilleromPersonId): List<Saksbehandlingsperiode> {
        return daoer.saksbehandlingsperiodeDao.finnPerioderForPerson(spilleromPersonId.personId)
    }

    fun sendTilBeslutning(
        periodeRef: SaksbehandlingsperiodeReferanse,
        saksbehandler: Bruker,
    ): Saksbehandlingsperiode {
        val periode = hentPeriode(periodeRef)
        krevAtBrukerErSaksbehandlerFor(saksbehandler, periode)
        val nyStatus = SaksbehandlingsperiodeStatus.TIL_BESLUTNING
        periode.verifiserNyStatusGyldighet(nyStatus)
        daoer.saksbehandlingsperiodeDao.endreStatus(periode, nyStatus = nyStatus)
        return periode.reload()
    }

    fun taTilBeslutning(
        periodeRef: SaksbehandlingsperiodeReferanse,
        saksbehandler: Bruker,
    ): Saksbehandlingsperiode {
        // TODO: krevAtBrukerErBeslutter() ? (verifiseres dog allerede i RolleMatrise)
        val periode = hentPeriode(periodeRef)
        val nyStatus = SaksbehandlingsperiodeStatus.UNDER_BESLUTNING
        periode.verifiserNyStatusGyldighet(nyStatus)
        daoer.saksbehandlingsperiodeDao.endreStatusOgBeslutter(
            periode,
            nyStatus = nyStatus,
            beslutterNavIdent = saksbehandler.navIdent,
        )
        return periode.reload()
    }

    fun sendTilbakeFraBeslutning(
        periodeRef: SaksbehandlingsperiodeReferanse,
        saksbehandler: Bruker,
    ): Saksbehandlingsperiode {
        val periode = hentPeriode(periodeRef)
        krevAtBrukerErBeslutterFor(saksbehandler, periode)

        val nyStatus = SaksbehandlingsperiodeStatus.UNDER_BEHANDLING
        periode.verifiserNyStatusGyldighet(nyStatus)
        daoer.saksbehandlingsperiodeDao.endreStatusOgBeslutter(
            periode,
            nyStatus = nyStatus,
            beslutterNavIdent = null,
        ) // TODO: Eller skal beslutter beholdes ? Jo, mest sannsynlig!
        return periode.reload()
    }

    fun godkjennPeriode(
        periodeRef: SaksbehandlingsperiodeReferanse,
        saksbehandler: Bruker,
    ): Saksbehandlingsperiode {
        val periode = hentPeriode(periodeRef)
        krevAtBrukerErBeslutterFor(saksbehandler, periode)

        val nyStatus = SaksbehandlingsperiodeStatus.GODKJENT
        periode.verifiserNyStatusGyldighet(nyStatus)
        daoer.saksbehandlingsperiodeDao.endreStatusOgBeslutter(
            periode,
            nyStatus = nyStatus,
            beslutterNavIdent = saksbehandler.navIdent,
        )
        return periode.reload()
    }
}

private fun krevAtBrukerErBeslutterFor(
    bruker: Bruker,
    periode: Saksbehandlingsperiode,
) {
    fun Bruker.erBeslutterFor(periode: Saksbehandlingsperiode): Boolean {
        return periode.beslutterNavIdent == this.navIdent
    }

    if (!bruker.erBeslutterFor(periode)) {
        throw ForbiddenException("Ikke beslutter for periode")
    }
}

private fun krevAtBrukerErSaksbehandlerFor(
    bruker: Bruker,
    periode: Saksbehandlingsperiode,
) {
    fun Bruker.erSaksbehandlerFor(periode: Saksbehandlingsperiode): Boolean {
        return periode.opprettetAvNavIdent == this.navIdent
    }

    if (!bruker.erSaksbehandlerFor(periode)) {
        throw ForbiddenException("Ikke saksbehandler for periode")
    }
}

private fun Saksbehandlingsperiode.verifiserNyStatusGyldighet(nyStatus: SaksbehandlingsperiodeStatus) {
    if (!SaksbehandlingsperiodeStatus.erGyldigEndring(status to nyStatus)) {
        throw InputValideringException("Ugyldig statusendring: $status til $nyStatus")
    }
}
