package no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import kotliquery.Row
import kotliquery.Session
import no.nav.helse.bakrommet.infrastruktur.db.MedDataSource
import no.nav.helse.bakrommet.infrastruktur.db.MedSession
import no.nav.helse.bakrommet.infrastruktur.db.QueryRunner
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.util.*
import java.time.OffsetDateTime
import java.util.UUID
import javax.sql.DataSource

typealias Kategorisering = JsonNode
typealias Dagoversikt = JsonNode

data class Inntektsforhold(
    val id: UUID,
    val kategorisering: Kategorisering,
    val kategoriseringGenerert: Kategorisering?,
    val dagoversikt: Dagoversikt?,
    val dagoversiktGenerert: Dagoversikt?,
    val saksbehandlingsperiodeId: UUID,
    val opprettet: OffsetDateTime,
    val generertFraDokumenter: List<UUID>,
)

class InntektsforholdDao private constructor(private val db: QueryRunner) {
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    constructor(session: Session) : this(MedSession(session))

    fun opprettInntektsforhold(inntektsforhold: Inntektsforhold): Inntektsforhold {
        db.update(
            """
            insert into inntektsforhold
                (id, kategorisering, kategorisering_generert,
                dagoversikt, dagoversikt_generert,
                saksbehandlingsperiode_id, opprettet, generert_fra_dokumenter)
            values
                (:id, :kategorisering, :kategorisering_generert,
                :dagoversikt, :dagoversikt_generert,
                :saksbehandlingsperiode_id, :opprettet, :generert_fra_dokumenter)
            """.trimIndent(),
            "id" to inntektsforhold.id,
            "kategorisering" to inntektsforhold.kategorisering.serialisertTilString(),
            "kategorisering_generert" to inntektsforhold.kategoriseringGenerert?.serialisertTilString(),
            "dagoversikt" to inntektsforhold.dagoversikt?.serialisertTilString(),
            "dagoversikt_generert" to inntektsforhold.dagoversiktGenerert?.serialisertTilString(),
            "saksbehandlingsperiode_id" to inntektsforhold.saksbehandlingsperiodeId,
            "opprettet" to inntektsforhold.opprettet,
            "generert_fra_dokumenter" to inntektsforhold.generertFraDokumenter.serialisertTilString(),
        )
        return hentInntektsforhold(inntektsforhold.id)!!
    }

    fun hentInntektsforhold(id: UUID): Inntektsforhold? =
        db.single(
            """
            select * from inntektsforhold where id = :id
            """.trimIndent(),
            "id" to id,
            mapper = ::inntektsforholdFraRow,
        )

    fun hentInntektsforholdFor(periode: Saksbehandlingsperiode): List<Inntektsforhold> =
        db.list(
            """
            select * from inntektsforhold where saksbehandlingsperiode_id = :behandling_id
            """.trimIndent(),
            "behandling_id" to periode.id,
            mapper = ::inntektsforholdFraRow,
        )

    private fun inntektsforholdFraRow(row: Row) =
        Inntektsforhold(
            id = row.uuid("id"),
            kategorisering = row.string("kategorisering").asJsonNode(),
            kategoriseringGenerert = row.stringOrNull("kategorisering_generert")?.asJsonNode(),
            dagoversikt = row.stringOrNull("dagoversikt")?.asJsonNode(),
            dagoversiktGenerert = row.stringOrNull("dagoversikt_generert")?.asJsonNode(),
            saksbehandlingsperiodeId = row.uuid("saksbehandlingsperiode_id"),
            opprettet = row.offsetDateTime("opprettet"),
            generertFraDokumenter =
                row
                    .stringOrNull("generert_fra_dokumenter")?.somListe<UUID>() ?: emptyList(),
        )

    fun oppdaterKategorisering(
        inntektsforhold: Inntektsforhold,
        kategorisering: JsonNode,
    ): Inntektsforhold {
        db.update(
            """
            update inntektsforhold set kategorisering = :kategorisering where id = :id
            """.trimIndent(),
            "id" to inntektsforhold.id,
            "kategorisering" to kategorisering.serialisertTilString(),
        )
        return hentInntektsforhold(inntektsforhold.id)!!
    }

    fun oppdaterDagoversikt(
        inntektsforhold: Inntektsforhold,
        oppdatertDagoversikt: ArrayNode,
    ): Inntektsforhold {
        db.update(
            """
            update inntektsforhold set dagoversikt = :dagoversikt where id = :id
            """.trimIndent(),
            "id" to inntektsforhold.id,
            "dagoversikt" to oppdatertDagoversikt.serialisertTilString(),
        )
        return hentInntektsforhold(inntektsforhold.id)!!
    }

    fun slettInntektsforhold(id: UUID) {
        db.update(
            """
            delete from inntektsforhold where id = :id
            """.trimIndent(),
            "id" to id,
        )
    }
}
