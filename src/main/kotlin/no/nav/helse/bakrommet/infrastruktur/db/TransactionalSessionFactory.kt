package no.nav.helse.bakrommet.infrastruktur.db

import kotliquery.Session
import kotliquery.sessionOf
import javax.sql.DataSource

class TransactionalSessionFactory<SessionDaosType>(private val dataSource: DataSource, private val daosCreatorFunction: (Session) -> SessionDaosType) {
    fun <RET> transactionalSessionScope(transactionalBlock: (SessionDaosType) -> RET): RET =
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            session.transaction { transactionalSession ->
                transactionalBlock(daosCreatorFunction(transactionalSession))
            }
        }
}

/*class FellesSessionDaoer(session: Session) {

}*/
