package no.nav.helse.bakrommet.db.dao

import kotliquery.Session
import no.nav.helse.bakrommet.db.MedDataSource
import no.nav.helse.bakrommet.db.MedSession
import no.nav.helse.bakrommet.db.QueryRunner
import no.nav.helse.bakrommet.domain.person.NaturligIdent
import no.nav.helse.bakrommet.person.PersonPseudoIdDao
import java.time.OffsetDateTime
import java.util.UUID
import javax.sql.DataSource

class PersonPseudoIdDaoPg private constructor(
    private val db: QueryRunner,
) : PersonPseudoIdDao {
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    constructor(session: Session) : this(MedSession(session))

    override fun opprettPseudoId(
        pseudoId: UUID,
        naturligIdent: NaturligIdent,
    ) {
        db.update(
            """
            INSERT INTO person_pseudo_id (pseudo_id, naturlig_ident, opprettet)
            VALUES (:pseudo_id, :naturlig_ident, NOW())
            """.trimIndent(),
            "pseudo_id" to pseudoId,
            "naturlig_ident" to naturligIdent.value,
        )
    }

    override fun finnNaturligIdent(pseudoId: UUID): NaturligIdent? =
        db
            .single(
                "SELECT naturlig_ident FROM person_pseudo_id WHERE pseudo_id = :pseudo_id ",
                "pseudo_id" to pseudoId,
            ) { it.string(1) }
            ?.let { NaturligIdent(it) }

    override fun finnPseudoID(naturligIdent: NaturligIdent): UUID? =
        db.single(
            "SELECT pseudo_id FROM person_pseudo_id WHERE naturlig_ident = :naturlig_ident AND opprettet > NOW() - INTERVAL '24 HOURS' ORDER BY opprettet DESC LIMIT 1",
            "naturlig_ident" to naturligIdent.value,
        ) { it.uuid("pseudo_id") }

    override fun slettPseudoIderEldreEnn(tidspunkt: OffsetDateTime): Int =
        db.update(
            "DELETE FROM person_pseudo_id WHERE opprettet < :tidspunkt",
            "tidspunkt" to tidspunkt,
        )
}
