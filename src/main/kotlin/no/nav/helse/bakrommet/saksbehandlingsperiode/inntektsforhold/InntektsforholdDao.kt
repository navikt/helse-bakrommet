package no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold

import com.fasterxml.jackson.databind.JsonNode
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

data class InntektsforholdDTO(
    val id: UUID,
    val kategorisering: JsonNode,
    val dagoversikt: JsonNode?,
    val generertFraDokumenter: List<UUID>,
)

fun Inntektsforhold.tilDto() =
    InntektsforholdDTO(
        id = id,
        kategorisering = kategorisering,
        dagoversikt = dagoversikt,
        generertFraDokumenter = generertFraDokumenter,
    )

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

    fun oppdaterDagoversiktDager(
        inntektsforhold: Inntektsforhold,
        dagerSomSkalOppdateresJson: JsonNode,
    ): Inntektsforhold {
        // Hent eksisterende dagoversikt
        val eksisterendeDagoversikt =
            inntektsforhold.dagoversikt?.let { dagoversiktJson ->
                if (dagoversiktJson.isArray) {
                    dagoversiktJson.toList()
                } else {
                    emptyList()
                }
            } ?: emptyList()

        // Opprett map for enkel oppslag basert på dato
        val eksisterendeDagerMap =
            eksisterendeDagoversikt.associateBy {
                it["dato"].asText()
            }.toMutableMap()

        // Oppdater kun dagene som finnes i input, ignorer helgedager
        if (dagerSomSkalOppdateresJson.isArray) {
            dagerSomSkalOppdateresJson.forEach { oppdatertDagJson ->
                val dato = oppdatertDagJson["dato"].asText()
                val eksisterendeDag = eksisterendeDagerMap[dato]

                if (eksisterendeDag != null && eksisterendeDag["dagtype"].asText() != "Helg") {
                    // Oppdater dagen og sett kilde til Saksbehandler
                    val oppdatertDag =
                        objectMapper.createObjectNode().apply {
                            set<JsonNode>("dato", oppdatertDagJson["dato"])
                            set<JsonNode>("dagtype", oppdatertDagJson["dagtype"])
                            set<JsonNode>("grad", oppdatertDagJson["grad"])
                            set<JsonNode>("avvistBegrunnelse", oppdatertDagJson["avvistBegrunnelse"])
                            put("kilde", "Saksbehandler")
                        }
                    eksisterendeDagerMap[dato] = oppdatertDag
                }
            }
        }

        // Konverter tilbake til JsonNode array og lagre
        val oppdatertDagoversikt =
            objectMapper.createArrayNode().apply {
                eksisterendeDagerMap.values.forEach { add(it) }
            }

        db.update(
            """
            update inntektsforhold set dagoversikt = :dagoversikt where id = :id
            """.trimIndent(),
            "id" to inntektsforhold.id,
            "dagoversikt" to oppdatertDagoversikt.serialisertTilString(),
        )
        return hentInntektsforhold(inntektsforhold.id)!!
    }
}
