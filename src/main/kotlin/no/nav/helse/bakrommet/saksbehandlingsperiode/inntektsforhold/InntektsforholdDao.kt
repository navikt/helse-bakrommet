package no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.Row
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.util.*
import java.time.OffsetDateTime
import java.util.UUID
import javax.sql.DataSource

data class InntektsforholdDTO(
    val id: UUID,
    val kategorisering: JsonNode,
    val sykmeldtFraForholdet: Boolean,
    val dagoversikt: JsonNode,
    val generertFraDokumenter: List<UUID>,
)

fun Inntektsforhold.tilDto() =
    InntektsforholdDTO(
        id = id,
        kategorisering = kategorisering,
        sykmeldtFraForholdet = sykmeldtFraForholdet,
        dagoversikt = dagoversikt,
        generertFraDokumenter = generertFraDokumenter,
    )

typealias Kategorisering = JsonNode
typealias Dagoversikt = JsonNode

data class Inntektsforhold(
    val id: UUID,
    val kategorisering: Kategorisering,
    val kategoriseringGenerert: Kategorisering?,
    val sykmeldtFraForholdet: Boolean,
    val dagoversikt: Dagoversikt,
    val dagoversiktGenerert: Dagoversikt?,
    val saksbehandlingsperiodeId: UUID,
    val opprettet: OffsetDateTime,
    val generertFraDokumenter: List<UUID>,
)

class InntektsforholdDao(private val dataSource: DataSource) {
    fun opprettInntektsforhold(inntektsforhold: Inntektsforhold): Inntektsforhold {
        dataSource.insert(
            """
            insert into inntektsforhold
                (id, kategorisering, kategorisering_generert, sykmeldt_fra_forholdet, 
                dagoversikt, dagoversikt_generert, 
                saksbehandlingsperiode_id, opprettet, generert_fra_dokumenter)
            values
                (:id, :kategorisering, :kategorisering_generert, :sykmeldt_fra_forholdet, 
                :dagoversikt, :dagoversikt_generert,
                :saksbehandlingsperiode_id, :opprettet, :generert_fra_dokumenter)
            """.trimIndent(),
            "id" to inntektsforhold.id,
            "kategorisering" to inntektsforhold.kategorisering.serialisertTilString(),
            "kategorisering_generert" to inntektsforhold.kategoriseringGenerert?.serialisertTilString(),
            "sykmeldt_fra_forholdet" to inntektsforhold.sykmeldtFraForholdet,
            "dagoversikt" to inntektsforhold.dagoversikt.serialisertTilString(),
            "dagoversikt_generert" to inntektsforhold.dagoversiktGenerert?.serialisertTilString(),
            "saksbehandlingsperiode_id" to inntektsforhold.saksbehandlingsperiodeId,
            "opprettet" to inntektsforhold.opprettet,
            "generert_fra_dokumenter" to inntektsforhold.generertFraDokumenter.serialisertTilString(),
        )
        return hentInntektsforhold(inntektsforhold.id)!!
    }

    fun hentInntektsforhold(id: UUID): Inntektsforhold? =
        dataSource.single(
            """
            select * from inntektsforhold where id = :id
            """.trimIndent(),
            "id" to id,
            mapper = ::inntektsforholdFraRow,
        )

    fun hentInntektsforholdFor(periode: Saksbehandlingsperiode): List<Inntektsforhold> =
        dataSource.list(
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
            sykmeldtFraForholdet = row.boolean("sykmeldt_fra_forholdet"),
            dagoversikt = row.string("dagoversikt").asJsonNode(),
            dagoversiktGenerert = row.stringOrNull("dagoversikt_generert")?.asJsonNode(),
            saksbehandlingsperiodeId = row.uuid("saksbehandlingsperiode_id"),
            opprettet = row.offsetDateTime("opprettet"),
            generertFraDokumenter =
                row
                    .stringOrNull("generert_fra_dokumenter")?.somListe<UUID>() ?: emptyList(),
        )
}
