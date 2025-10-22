package no.nav.helse.bakrommet.person

import kotliquery.Session
import no.nav.helse.bakrommet.infrastruktur.db.MedDataSource
import no.nav.helse.bakrommet.infrastruktur.db.MedSession
import no.nav.helse.bakrommet.infrastruktur.db.QueryRunner
import javax.sql.DataSource

class PersonDao private constructor(
    private val db: QueryRunner,
) {
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    constructor(session: Session) : this(MedSession(session))

    fun finnPersonId(vararg identer: String): String? {
        val params = identer.mapIndexed { i, id -> "p$i" to id }
        val placeholderList = params.joinToString(",") { ":${it.first}" }

        val sql = """
      SELECT spillerom_id
      FROM ident
      WHERE naturlig_ident IN ($placeholderList)
    """

        return db.single(sql, *params.toTypedArray()) { rs ->
            rs.string("spillerom_id")
        }
    }

    fun opprettPerson(
        naturligIdent: String,
        spilleromId: String,
    ) {
        db.update(
            """
            insert into ident (spillerom_id, naturlig_ident)
            values (:spillerom_id, :naturlig_ident)
            """.trimIndent(),
            "naturlig_ident" to naturligIdent,
            "spillerom_id" to spilleromId,
        )
    }

    fun finnNaturligIdent(spilleromId: String): String? =
        db.single(
            "select naturlig_ident from ident where spillerom_id = :spillerom_id",
            "spillerom_id" to spilleromId,
        ) { it.string(1) }

    fun hentNaturligIdent(spilleromId: String): String = finnNaturligIdent(spilleromId) ?: throw RuntimeException("Fant ikke person med spilleromId $spilleromId")
}
