package no.nav.helse.bakrommet.db

import kotliquery.Session
import kotliquery.sessionOf
import no.nav.helse.bakrommet.infrastruktur.db.TransactionalSessionFactory
import javax.sql.DataSource

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
