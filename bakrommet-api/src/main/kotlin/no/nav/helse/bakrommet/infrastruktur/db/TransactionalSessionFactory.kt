package no.nav.helse.bakrommet.infrastruktur.db

import kotliquery.Session
import kotliquery.sessionOf
import javax.sql.DataSource

interface TransactionalSessionFactory<out SessionDaosType> {
    suspend fun <RET> transactionalSessionScope(transactionalBlock: suspend (SessionDaosType) -> RET): RET
}

class TransactionalSessionFactoryPg<out SessionDaosType>(
    private val dataSource: DataSource,
    private val daosCreatorFunction: (Session) -> SessionDaosType,
) : TransactionalSessionFactory<SessionDaosType> {
    override suspend fun <RET> transactionalSessionScope(transactionalBlock: suspend (SessionDaosType) -> RET): RET =
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            session.transaction { transactionalSession ->
                transactionalBlock(daosCreatorFunction(transactionalSession))
            }
        }
}

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
        eksisterendeTransaksjon: RET?,
        block: suspend (DaosType.() -> RET),
    ): RET {
        eksisterendeTransaksjon?.let {
            return block(daoer)
        }

        return sessionFactory.transactionalSessionScope { session ->
            block(session)
        }
    }
}

interface DbDaoer<out DaosType> {
    suspend fun <RET> nonTransactional(block: suspend (DaosType.() -> RET)): RET

    suspend fun <RET> transactional(
        eksisterendeTransaksjon: RET? = null,
        block: suspend (DaosType.() -> RET),
    ): RET
}
