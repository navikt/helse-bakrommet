package no.nav.helse.bakrommet.person

import no.nav.helse.bakrommet.util.single
import no.nav.helse.bakrommet.util.update
import javax.sql.DataSource

class PersonDao(private val dataSource: DataSource) {
    fun finnPersonId(vararg identer: String): String? {
        val params = identer.mapIndexed { i, id -> "p$i" to id }
        val placeholderList = params.joinToString(",") { ":${it.first}" }

        val sql = """
      SELECT spillerom_id
      FROM ident
      WHERE naturlig_ident IN ($placeholderList)
    """

        return dataSource.single(sql, *params.toTypedArray()) { rs ->
            rs.string("spillerom_id")
        }
    }

    fun opprettPerson(
        naturligIdent: String,
        spilleromId: String,
    ) {
        dataSource.update(
            """
            insert into ident (spillerom_id, naturlig_ident)
            values (:spillerom_id, :naturlig_ident)
            """.trimIndent(),
            "naturlig_ident" to naturligIdent,
            "spillerom_id" to spilleromId,
        )
    }

    fun finnNaturligIdent(spilleromId: String): String? {
        return dataSource.single(
            "select naturlig_ident from ident where spillerom_id = :spillerom_id",
            "spillerom_id" to spilleromId,
        ) { it.string(1) }
    }
}
