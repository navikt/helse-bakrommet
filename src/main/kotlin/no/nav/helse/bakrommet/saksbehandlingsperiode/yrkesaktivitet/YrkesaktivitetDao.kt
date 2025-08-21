package no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet

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

data class Yrkesaktivitet(
    val id: UUID,
    val kategorisering: Kategorisering,
    val kategoriseringGenerert: Kategorisering?,
    val dagoversikt: Dagoversikt?,
    val dagoversiktGenerert: Dagoversikt?,
    val saksbehandlingsperiodeId: UUID,
    val opprettet: OffsetDateTime,
    val generertFraDokumenter: List<UUID>,
)

class YrkesaktivitetDao private constructor(private val db: QueryRunner) {
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    constructor(session: Session) : this(MedSession(session))

    fun opprettYrkesaktivitet(yrkesaktivitet: Yrkesaktivitet): Yrkesaktivitet {
        db.update(
            """
            insert into yrkesaktivitet
                (id, kategorisering, kategorisering_generert,
                dagoversikt, dagoversikt_generert,
                saksbehandlingsperiode_id, opprettet, generert_fra_dokumenter)
            values
                (:id, :kategorisering, :kategorisering_generert,
                :dagoversikt, :dagoversikt_generert,
                :saksbehandlingsperiode_id, :opprettet, :generert_fra_dokumenter)
            """.trimIndent(),
            "id" to yrkesaktivitet.id,
            "kategorisering" to yrkesaktivitet.kategorisering.serialisertTilString(),
            "kategorisering_generert" to yrkesaktivitet.kategoriseringGenerert?.serialisertTilString(),
            "dagoversikt" to yrkesaktivitet.dagoversikt?.serialisertTilString(),
            "dagoversikt_generert" to yrkesaktivitet.dagoversiktGenerert?.serialisertTilString(),
            "saksbehandlingsperiode_id" to yrkesaktivitet.saksbehandlingsperiodeId,
            "opprettet" to yrkesaktivitet.opprettet,
            "generert_fra_dokumenter" to yrkesaktivitet.generertFraDokumenter.serialisertTilString(),
        )
        return hentYrkesaktivitet(yrkesaktivitet.id)!!
    }

    fun hentYrkesaktivitet(id: UUID): Yrkesaktivitet? =
        db.single(
            """
            select * from yrkesaktivitet where id = :id
            """.trimIndent(),
            "id" to id,
            mapper = ::yrkesaktivitetFraRow,
        )

    fun hentYrkesaktivitetFor(periode: Saksbehandlingsperiode): List<Yrkesaktivitet> =
        db.list(
            """
            select * from yrkesaktivitet where saksbehandlingsperiode_id = :behandling_id
            """.trimIndent(),
            "behandling_id" to periode.id,
            mapper = ::yrkesaktivitetFraRow,
        )

    private fun yrkesaktivitetFraRow(row: Row) =
        Yrkesaktivitet(
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
        yrkesaktivitet: Yrkesaktivitet,
        kategorisering: JsonNode,
    ): Yrkesaktivitet {
        db.update(
            """
            update yrkesaktivitet set kategorisering = :kategorisering where id = :id
            """.trimIndent(),
            "id" to yrkesaktivitet.id,
            "kategorisering" to kategorisering.serialisertTilString(),
        )
        return hentYrkesaktivitet(yrkesaktivitet.id)!!
    }

    fun oppdaterDagoversikt(
        yrkesaktivitet: Yrkesaktivitet,
        oppdatertDagoversikt: ArrayNode,
    ): Yrkesaktivitet {
        db.update(
            """
            update yrkesaktivitet set dagoversikt = :dagoversikt where id = :id
            """.trimIndent(),
            "id" to yrkesaktivitet.id,
            "dagoversikt" to oppdatertDagoversikt.serialisertTilString(),
        )
        return hentYrkesaktivitet(yrkesaktivitet.id)!!
    }
}
