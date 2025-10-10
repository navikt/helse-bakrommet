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
    val aInntekt828 = "ainntekt828"
    val arbeidsforhold = "arbeidsforhold"
    val pensjonsgivendeinntekt = "pensjonsgivendeinntekt"
}

data class Dokument(
    val id: UUID = UUID.randomUUID(),
    val dokumentType: String,
    val eksternId: String?,
    val innhold: String,
    val opprettet: Instant = Instant.now(),
    val request: Kildespor,
    val opprettetForBehandling: UUID,
) {
    fun somSøknad(): SykepengesoknadDTO {
        check(dokumentType == DokumentType.søknad)
        return objectMapper.readValue<SykepengesoknadDTO>(innhold)
    }
}

class DokumentDao private constructor(
    private val db: QueryRunner,
) {
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    constructor(session: Session) : this(MedSession(session))

    fun opprettDokument(dokument: Dokument): Dokument {
        db.update(
            """
            insert into dokument
                (id, dokument_type, ekstern_id, innhold, opprettet, request, opprettet_for_behandling)
            values
                (:id, :dokument_type, :ekstern_id, :innhold, :opprettet, :request, :opprettet_for_behandling)
            """.trimIndent(),
            "id" to dokument.id,
            "dokument_type" to dokument.dokumentType,
            "ekstern_id" to dokument.eksternId,
            "innhold" to dokument.innhold,
            "opprettet" to dokument.opprettet,
            "request" to dokument.request.kilde,
            "opprettet_for_behandling" to dokument.opprettetForBehandling,
        )
        return hentDokument(dokument.id)!!
    }

    fun hentDokument(id: UUID): Dokument? =
        db.single(
            """
            select * from dokument where id = :id
            """.trimIndent(),
            "id" to id,
            mapper = ::dokumentFraRow,
        )

    fun hentDokumenterFor(behandlingId: UUID): List<Dokument> =
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
            request = Kildespor(kilde = row.string("request")),
            opprettetForBehandling = row.uuid("opprettet_for_behandling"),
        )
}
