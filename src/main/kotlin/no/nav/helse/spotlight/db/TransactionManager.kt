package no.nav.helse.spotlight.db

import kotliquery.sessionOf
import javax.sql.DataSource

class TransactionManager(private val dataSource: DataSource) {
    fun <T> transaction(block: (dao: SuspendertKommandokjedeDao) -> T): T =
        sessionOf(dataSource, strict = true).use { session ->
            session.transaction { tx ->
                block(SuspendertKommandokjedeDao(SqlRunner(tx)))
            }
        }
}
