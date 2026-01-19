package no.nav.helse.bakrommet.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.action.QueryAction
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.bakrommet.util.serialisertTilString
import org.intellij.lang.annotations.Language
import org.postgresql.util.PGobject
import javax.sql.DataSource

sealed class QueryRunner protected constructor() {
    fun <T> single(
        @Language("PostgreSQL")
        sql: String,
        vararg params: Pair<String, Any>,
        mapper: (Row) -> T?,
    ): T? = run(queryOf(sql, params.toMap()).map(mapper).asSingle)

    fun update(
        @Language("PostgreSQL")
        sql: String,
        vararg params: Pair<String, Any?>,
    ): Int = run(queryOf(sql, params.toMap()).asUpdate)

    fun execute(
        @Language("PostgreSQL") sql: String,
    ): Boolean = run(queryOf(sql).asExecute)

    fun <T> list(
        @Language("PostgreSQL")
        sql: String,
        vararg params: Pair<String, Any>,
        mapper: (Row) -> T,
    ): List<T> =
        run(
            queryOf(sql, params.toMap())
                .map(mapper)
                .asList,
        )

    protected abstract fun <A> run(action: QueryAction<A>): A
}

class MedSession(
    private val session: Session,
) : QueryRunner() {
    override fun <A> run(action: QueryAction<A>): A = action.runWithSession(session)
}

class MedDataSource(
    private val dataSource: DataSource,
) : QueryRunner() {
    override fun <A> run(action: QueryAction<A>): A =
        sessionOf(dataSource = dataSource, strict = true).use { session ->
            action.runWithSession(session)
        }
}

fun Any.tilPgJson(): PGobject {
    val innhold = this
    return PGobject().apply {
        type = "json"
        value = innhold.serialisertTilString()
    }
}

fun String.tilPgJson(): PGobject {
    val innhold = this
    return PGobject().apply {
        type = "json"
        value = innhold
    }
}
