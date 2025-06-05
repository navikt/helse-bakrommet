package no.nav.helse.bakrommet.util

import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import javax.sql.DataSource

fun <T> DataSource.single(
    @Language("postgresql") sql: String,
    vararg params: Pair<String, Any>,
    mapper: (Row) -> T?,
) = sessionOf(this, strict = true).use { session ->
    session.run(queryOf(sql, params.toMap()).map(mapper).asSingle)
}

fun DataSource.insert(
    @Language("postgresql") sql: String,
    vararg params: Pair<String, Any?>,
) = sessionOf(this, strict = true).use { session -> session.run(queryOf(sql, params.toMap()).asUpdate) }

fun DataSource.execute(
    @Language("postgresql") sql: String,
) = sessionOf(this, strict = true).use { session -> session.run(queryOf(sql).asExecute) }

fun <T> Iterable<T>?.somDbArray(transform: (T) -> CharSequence = { it.toString() }) =
    this?.joinToString(prefix = "{", postfix = "}", transform = transform) ?: "{}"

fun <T> DataSource.list(
    @Language("postgresql") sql: String,
    vararg params: Pair<String, Any>,
    mapper: (Row) -> T,
): List<T> =
    sessionOf(this, strict = true).use { session ->
        session.run(
            queryOf(sql, params.toMap())
                .map(mapper)
                .asList,
        )
    }
