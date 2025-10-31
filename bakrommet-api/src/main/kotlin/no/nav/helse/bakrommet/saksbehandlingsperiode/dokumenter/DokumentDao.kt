package no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.Session
import no.nav.helse.bakrommet.infrastruktur.db.MedDataSource
import no.nav.helse.bakrommet.infrastruktur.db.MedSession
import no.nav.helse.bakrommet.infrastruktur.db.QueryRunner
import no.nav.helse.bakrommet.util.*
import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
import java.time.Instant
import java.util.*
import javax.sql.DataSource

object DokumentType {
    val søknad = "søknad"
    val inntektsmelding = "inntektsmelding"
    val aInntekt828 = "ainntekt828"
    val aInntekt830 = "ainntekt830"
    val arbeidsforhold = "arbeidsforhold"
    val pensjonsgivendeinntekt = "pensjonsgivendeinntekt"
}

data class Dokument(
    val id: UUID = UUID.randomUUID(),
    val dokumentType: String,
    val eksternId: String?,
    val innhold: String,
    val opprettet: Instant = Instant.now(),
    val sporing: Kildespor,
    val forespurteData: String? = null,
    val opprettetForBehandling: UUID,
) {
    fun somSøknad(): SykepengesoknadDTO {
        check(dokumentType == DokumentType.søknad)
        return objectMapper.readValue<SykepengesoknadDTO>(innhold)
    }
}

interface DokumentDao {
    fun finnDokumentMedEksternId(
        saksbehandlingsperiodeId: UUID,
        dokumentType: String,
        eksternId: String,
    ): Dokument?

    fun finnDokumentForForespurteData(
        saksbehandlingsperiodeId: UUID,
        dokumentType: String,
        forespurteData: String,
    ): Dokument?

    fun opprettDokument(dokument: Dokument): Dokument

    fun hentDokument(id: UUID): Dokument?

    fun hentDokumenterFor(behandlingId: UUID): List<Dokument>
}

class DokumentDaoPg private constructor(
    private val db: QueryRunner,
) : DokumentDao {
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    constructor(session: Session) : this(MedSession(session))

    override fun finnDokumentMedEksternId(
        saksbehandlingsperiodeId: UUID,
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
            "opprettet_for_behandling" to saksbehandlingsperiodeId,
            "dokument_type" to dokumentType,
            "ekstern_id" to eksternId,
            mapper = ::dokumentFraRow,
        )

    override fun finnDokumentForForespurteData(
        saksbehandlingsperiodeId: UUID,
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
            "opprettet_for_behandling" to saksbehandlingsperiodeId,
            "dokument_type" to dokumentType,
            "forespurte_data" to forespurteData,
            mapper = ::dokumentFraRow,
        )

    override fun opprettDokument(dokument: Dokument): Dokument {
        db.update(
            """
            insert into dokument
                (id, dokument_type, ekstern_id, innhold, opprettet, sporing, opprettet_for_behandling, forespurte_data)
            values
                (:id, :dokument_type, :ekstern_id, :innhold, :opprettet, :sporing, :opprettet_for_behandling, :forespurte_data)
            """.trimIndent(),
            "id" to dokument.id,
            "dokument_type" to dokument.dokumentType,
            "ekstern_id" to dokument.eksternId,
            "innhold" to dokument.innhold,
            "opprettet" to dokument.opprettet,
            "sporing" to dokument.sporing.kilde,
            "opprettet_for_behandling" to dokument.opprettetForBehandling,
            "forespurte_data" to dokument.forespurteData,
        )
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
