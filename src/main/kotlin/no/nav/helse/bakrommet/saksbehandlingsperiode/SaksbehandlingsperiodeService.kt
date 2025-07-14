package no.nav.helse.bakrommet.saksbehandlingsperiode

import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.errorhandling.InputValideringException
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
    val personId: SpilleromPersonId,
    val periodeUUID: UUID,
)

class SaksbehandlingsperiodeService(
    private val daoer: SaksbehandlingsperiodeServiceDaoer,
    private val sessionFactory: TransactionalSessionFactory<SaksbehandlingsperiodeServiceDaoer>,
    private val dokumentHenter: DokumentHenter,
) {
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
}
