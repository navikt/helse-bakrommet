package no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.bakrommet.infrastruktur.db.MedDataSource
import no.nav.helse.bakrommet.infrastruktur.db.MedSession
import no.nav.helse.bakrommet.infrastruktur.db.QueryRunner
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dag
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.tilDagoversikt
import no.nav.helse.bakrommet.util.*
import no.nav.helse.hendelser.Periode
import java.time.OffsetDateTime
import java.util.UUID
import javax.sql.DataSource

data class Yrkesaktivitet(
    val id: UUID,
    val kategorisering: Map<String, String>,
    val kategoriseringGenerert: Map<String, String>?,
    val dagoversikt: List<Dag>?,
    val dagoversiktGenerert: List<Dag>?,
    val saksbehandlingsperiodeId: UUID,
    val opprettet: OffsetDateTime,
    val generertFraDokumenter: List<UUID>,
    val perioder: Perioder? = null,
) {
    fun hentPerioderForType(periodetype: Periodetype): List<Periode> {
        return if (this.perioder?.type == periodetype) {
            this.perioder.perioder.map { Periode(it.fom, it.tom) }
        } else {
            emptyList()
        }
    }
}

class YrkesaktivitetDao private constructor(private val db: QueryRunner) {
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    constructor(session: Session) : this(MedSession(session))

    fun opprettYrkesaktivitet(yrkesaktivitet: Yrkesaktivitet): Yrkesaktivitet {
        db.update(
            """
            insert into yrkesaktivitet
                (id, kategorisering, kategorisering_generert,
                dagoversikt, dagoversikt_generert,
                saksbehandlingsperiode_id, opprettet, generert_fra_dokumenter, perioder)
            values
                (:id, :kategorisering, :kategorisering_generert,
                :dagoversikt, :dagoversikt_generert,
                :saksbehandlingsperiode_id, :opprettet, :generert_fra_dokumenter, :perioder)
            """.trimIndent(),
            "id" to yrkesaktivitet.id,
            "kategorisering" to yrkesaktivitet.kategorisering.serialisertTilString(),
            "kategorisering_generert" to yrkesaktivitet.kategoriseringGenerert?.serialisertTilString(),
            "dagoversikt" to yrkesaktivitet.dagoversikt?.serialisertTilString(),
            "dagoversikt_generert" to yrkesaktivitet.dagoversiktGenerert?.serialisertTilString(),
            "saksbehandlingsperiode_id" to yrkesaktivitet.saksbehandlingsperiodeId,
            "opprettet" to yrkesaktivitet.opprettet,
            "generert_fra_dokumenter" to yrkesaktivitet.generertFraDokumenter.serialisertTilString(),
            "perioder" to yrkesaktivitet.perioder?.serialisertTilString(),
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
            kategorisering = row.string("kategorisering").asStringStringMap(),
            kategoriseringGenerert = row.stringOrNull("kategorisering_generert")?.asStringStringMap(),
            dagoversikt = row.stringOrNull("dagoversikt")?.tilDagoversikt(),
            dagoversiktGenerert = row.stringOrNull("dagoversikt_generert")?.tilDagoversikt(),
            saksbehandlingsperiodeId = row.uuid("saksbehandlingsperiode_id"),
            opprettet = row.offsetDateTime("opprettet"),
            generertFraDokumenter =
                row
                    .stringOrNull("generert_fra_dokumenter")?.somListe<UUID>() ?: emptyList(),
            perioder = row.stringOrNull("perioder")?.let { objectMapper.readValue(it, Perioder::class.java) },
        )

    fun oppdaterKategorisering(
        yrkesaktivitet: Yrkesaktivitet,
        kategorisering: Map<String, String>,
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
        oppdatertDagoversikt: List<Dag>,
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

    fun oppdaterPerioder(
        yrkesaktivitet: Yrkesaktivitet,
        perioder: Perioder?,
    ): Yrkesaktivitet {
        db.update(
            """
            update yrkesaktivitet set perioder = :perioder where id = :id
            """.trimIndent(),
            "id" to yrkesaktivitet.id,
            "perioder" to perioder?.serialisertTilString(),
        )
        return hentYrkesaktivitet(yrkesaktivitet.id)!!
    }

    fun slettYrkesaktivitet(id: UUID) {
        db.update(
            """
            delete from yrkesaktivitet where id = :id
            """.trimIndent(),
            "id" to id,
        )
    }
}
