package no.nav.helse.bakrommet.person

import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.util.logg
import java.util.UUID

interface PersonsokDaoer {
    val personPseudoIdDao: PersonPseudoIdDao
}

class PersonsøkService(
    private val db: DbDaoer<PersonsokDaoer>,
) {
    suspend fun hentEllerOpprettPseudoId(
        naturligIdent: NaturligIdent,
    ): UUID {
        db
            .nonTransactional {
                personPseudoIdDao.finnPseudoID(naturligIdent)
            }?.let { eksisterendePersonId ->
                logg.info("Fant eksisterende personPseudoId for naturligIdent ${naturligIdent.naturligIdent}")
                return eksisterendePersonId
            }

        val nyPseudoId = UUID.randomUUID()

        db.nonTransactional {
            personPseudoIdDao.opprettPseudoId(nyPseudoId, naturligIdent)
        }
        return nyPseudoId
    }
}
