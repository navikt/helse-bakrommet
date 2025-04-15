package no.nav.helse.spotlight.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import org.intellij.lang.annotations.Language

class SqlRunner(private val session: Session) {
    fun update(
        @Language("postgresql") sql: String,
        params: Map<String, Any?>,
    ): Int = session.run(queryOf(sql, params).asUpdate)

    fun <T> queryList(
        @Language("postgresql") sql: String,
        params: Map<String, Any?> = emptyMap(),
        mapper: (Row) -> T,
    ): List<T> = session.run(queryOf(sql, params).map(mapper).asList)

    fun <T> querySingle(
        @Language("postgresql") sql: String,
        params: Map<String, Any?> = emptyMap(),
        mapper: (Row) -> T,
    ): T? = session.run(queryOf(sql, params).map(mapper).asSingle)
}
