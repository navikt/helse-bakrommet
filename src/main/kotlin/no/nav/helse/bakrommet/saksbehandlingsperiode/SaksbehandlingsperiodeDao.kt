package no.nav.helse.bakrommet.saksbehandlingsperiode

import kotliquery.Row
import no.nav.helse.bakrommet.util.insert
import no.nav.helse.bakrommet.util.list
import no.nav.helse.bakrommet.util.single
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import javax.sql.DataSource

data class Saksbehandlingsperiode(
    val id: UUID,
    val spilleromPersonId: String,
    val opprettet: OffsetDateTime,
    val opprettetAvNavIdent: String,
    val opprettetAvNavn: String,
    val fom: LocalDate,
    val tom: LocalDate,
)

class SaksbehandlingsperiodeDao(private val dataSource: DataSource) {
    fun finnSaksbehandlingsperiode(id: UUID): Saksbehandlingsperiode? =
        dataSource.single(
            """
            select *
              from saksbehandlingsperiode
             where id = :id
            """.trimIndent(),
            "id" to id,
        ) { rowTilPeriode(it) }

    // Ny metode: finn én periode basert på spillerom_personid
    fun finnPerioderForPerson(spilleromPersonId: String): List<Saksbehandlingsperiode> =
        dataSource.list(
            """
            select *
              from saksbehandlingsperiode
             where spillerom_personid = :spillerom_personid
            """.trimIndent(),
            "spillerom_personid" to spilleromPersonId,
        ) { rowTilPeriode(it) }

    private fun rowTilPeriode(row: Row) =
        Saksbehandlingsperiode(
            id = row.uuid("id"),
            spilleromPersonId = row.string("spillerom_personid"),
            opprettet = row.offsetDateTime("opprettet"),
            opprettetAvNavIdent = row.string("opprettet_av_nav_ident"),
            opprettetAvNavn = row.string("opprettet_av_navn"),
            fom = row.localDate("fom"),
            tom = row.localDate("tom"),
        )

    fun opprettPeriode(periode: Saksbehandlingsperiode) {
        dataSource.insert(
            """
            insert into saksbehandlingsperiode
                (id, spillerom_personid, opprettet, opprettet_av_nav_ident, opprettet_av_navn, fom, tom)
            values
                (:id, :spillerom_personid, :opprettet, :opprettet_av_nav_ident, :opprettet_av_navn, :fom, :tom)
            """.trimIndent(),
            "id" to periode.id,
            "spillerom_personid" to periode.spilleromPersonId,
            "opprettet" to periode.opprettet,
            "opprettet_av_nav_ident" to periode.opprettetAvNavIdent,
            "opprettet_av_navn" to periode.opprettetAvNavn,
            "fom" to periode.fom,
            "tom" to periode.tom,
        )
    }
}
