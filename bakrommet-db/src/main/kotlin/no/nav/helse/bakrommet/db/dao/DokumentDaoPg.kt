package no.nav.helse.bakrommet.db.dao

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.bakrommet.behandling.dokumenter.Dokument
import no.nav.helse.bakrommet.behandling.dokumenter.DokumentDao
import no.nav.helse.bakrommet.db.MedDataSource
import no.nav.helse.bakrommet.db.MedSession
import no.nav.helse.bakrommet.db.QueryRunner
import no.nav.helse.bakrommet.errorhandling.KunneIkkeOppdatereDbException
import no.nav.helse.bakrommet.util.Kildespor
import java.util.*
import javax.sql.DataSource

private val verifiserOppdatert: (Int) -> Unit = {
    if (it == 0) {
        throw KunneIkkeOppdatereDbException("Dokument kunne ikke oppdateres")
    }
}

// Ved eventuell delete/update: private const val AND_ER_UNDER_BEHANDLING = "AND (select status from behandling where behandling.id = dokument.opprettet_for_behandling) = '$STATUS_UNDER_BEHANDLING_STR'"
private const val WHERE_ER_UNDER_BEHANDLING_FOR_INSERT = "WHERE EXISTS (select 1 from behandling where behandling.id = :opprettet_for_behandling and status = '${STATUS_UNDER_BEHANDLING_STR}')"

class DokumentDaoPg private constructor(
    private val db: QueryRunner,
) : DokumentDao {
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    constructor(session: Session) : this(MedSession(session))

    override fun finnDokumentMedEksternId(
        behandlingId: UUID,
        dokumentType: String,
        eksternId: String,
    ): Dokument? =
        db.single(
            """
            select * from dokument 
            where opprettet_for_behandling = :opprettet_for_behandling
            and dokument_type = :dokument_type
            and ekstern_id = :ekstern_id
            """.trimIndent(),
            "opprettet_for_behandling" to behandlingId,
            "dokument_type" to dokumentType,
            "ekstern_id" to eksternId,
            mapper = ::dokumentFraRow,
        )

    override fun finnDokumentForForespurteData(
        behandlingId: UUID,
        dokumentType: String,
        forespurteData: String,
    ): Dokument? =
        db.single(
            """
            select * from dokument 
            where opprettet_for_behandling = :opprettet_for_behandling
            and dokument_type = :dokument_type
            and forespurte_data = :forespurte_data
            """.trimIndent(),
            "opprettet_for_behandling" to behandlingId,
            "dokument_type" to dokumentType,
            "forespurte_data" to forespurteData,
            mapper = ::dokumentFraRow,
        )

    override fun opprettDokument(dokument: Dokument): Dokument {
        db
            .update(
                """
                insert into dokument
                    (id, dokument_type, ekstern_id, innhold, opprettet, sporing, opprettet_for_behandling, forespurte_data)
                select
                    :id, :dokument_type, :ekstern_id, :innhold, :opprettet, :sporing, :opprettet_for_behandling, :forespurte_data
                $WHERE_ER_UNDER_BEHANDLING_FOR_INSERT
                """.trimIndent(),
                "id" to dokument.id,
                "dokument_type" to dokument.dokumentType,
                "ekstern_id" to dokument.eksternId,
                "innhold" to dokument.innhold,
                "opprettet" to dokument.opprettet,
                "sporing" to dokument.sporing.kilde,
                "opprettet_for_behandling" to dokument.opprettetForBehandling,
                "forespurte_data" to dokument.forespurteData,
            ).also(verifiserOppdatert)
        return hentDokument(dokument.id)!!
    }

    override fun hentDokument(id: UUID): Dokument? =
        db.single(
            """
            select * from dokument where id = :id
            """.trimIndent(),
            "id" to id,
            mapper = ::dokumentFraRow,
        )

    override fun hentDokumenterFor(behandlingId: UUID): List<Dokument> =
        db.list(
            """
            select * from dokument where opprettet_for_behandling = :behandling_id
            """.trimIndent(),
            "behandling_id" to behandlingId,
            mapper = ::dokumentFraRow,
        )

    private fun dokumentFraRow(row: Row) =
        Dokument(
            id = row.uuid("id"),
            dokumentType = row.string("dokument_type"),
            eksternId = row.stringOrNull("ekstern_id"),
            innhold = row.string("innhold"),
            opprettet = row.instant("opprettet"),
            sporing = Kildespor(kilde = row.string("sporing")),
            opprettetForBehandling = row.uuid("opprettet_for_behandling"),
            forespurteData = row.stringOrNull("forespurte_data"),
        )
}
