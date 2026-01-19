package no.nav.helse.bakrommet.person

import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.domain.person.NaturligIdent
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.infrastruktur.provider.PdlIdent
import no.nav.helse.bakrommet.infrastruktur.provider.PersoninfoProvider
import no.nav.helse.bakrommet.logg
import java.util.UUID

interface PersonsokDaoer {
    val personPseudoIdDao: PersonPseudoIdDao
}

class PersonsøkService(
    private val db: DbDaoer<PersonsokDaoer>,
    private val personinfoProvider: PersoninfoProvider,
) {
    suspend fun hentEllerOpprettPseudoId(
        naturligIdent: NaturligIdent,
    ): UUID {
        db
            .nonTransactional {
                personPseudoIdDao.finnPseudoID(naturligIdent)
            }?.let { eksisterendePersonId ->
                logg.info("Fant eksisterende personPseudoId for naturligIdent ${naturligIdent.value}")
                return eksisterendePersonId
            }

        val nyPseudoId = UUID.randomUUID()

        db.nonTransactional {
            personPseudoIdDao.opprettPseudoId(nyPseudoId, naturligIdent)
        }
        return nyPseudoId
    }

    suspend fun hentIdenter(
        naturligIdent: NaturligIdent,
        saksbehandler: BrukerOgToken,
    ): List<PdlIdent> = personinfoProvider.hentIdenterFor(saksbehandlerToken = saksbehandler.token, ident = naturligIdent)
}
