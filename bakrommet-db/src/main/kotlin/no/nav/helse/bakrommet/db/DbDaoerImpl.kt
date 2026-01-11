package no.nav.helse.bakrommet.db

import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.infrastruktur.db.TransactionalSessionFactory

/**
 * Convenience-wrapper rundt et objekt med ikke-transaksjonelle DAOer, og en TransactionalSessionFactory basert på samme interface,
 * som bl.a. kan sikre at man kun forholder seg til nonTransactional eller Transactional- versjon
 * av et Daoer-interface av gangen (per kode-blokk), ved at riktig versjon av DAO-samlingen tilgjengeliggjøres som "this"
 * (og ved at "daoer" og "sessionFactory" da ikke trenger være tilgjengelig i Servicen som "val"-verdier).
 */
class DbDaoerImpl<out DaosType>(
    private val daoer: DaosType,
    private val sessionFactory: TransactionalSessionFactory<DaosType>,
) : DbDaoer<DaosType> {
    override suspend fun <RET> nonTransactional(block: suspend (DaosType.() -> RET)): RET = block(daoer)

    override suspend fun <RET> transactional(
        eksisterendeTransaksjon: @UnsafeVariance DaosType?,
        block: suspend (DaosType.() -> RET),
    ): RET {
        eksisterendeTransaksjon?.let {
            return block(it)
        }

        return sessionFactory.transactionalSessionScope { session ->
            block(session)
        }
    }
}
