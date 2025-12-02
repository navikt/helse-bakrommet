package no.nav.helse.bakrommet.person

import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer

interface PersonIdServiceDaoer {
    val personDao: PersonDao
}

class PersonIdService(
    private val db: DbDaoer<PersonIdServiceDaoer>,
) {
    suspend fun finnNaturligIdent(spilleromId: String): String? =
        db.nonTransactional {
            personDao.finnNaturligIdent(spilleromId)
        }
}
