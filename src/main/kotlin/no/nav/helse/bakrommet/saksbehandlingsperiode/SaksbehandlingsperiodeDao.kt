package no.nav.helse.bakrommet.saksbehandlingsperiode

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.Row
import no.nav.helse.bakrommet.saksbehandlingsperiode.vilkaar.Kode
import no.nav.helse.bakrommet.saksbehandlingsperiode.vilkaar.OpprettetEllerEndret
import no.nav.helse.bakrommet.saksbehandlingsperiode.vilkaar.VurdertVilkårDao
import no.nav.helse.bakrommet.util.list
import no.nav.helse.bakrommet.util.single
import no.nav.helse.bakrommet.util.update
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
    val status: SaksbehandlingsperiodeStatus = SaksbehandlingsperiodeStatus.UNDER_BEHANDLING,
    val beslutter: String? = null,
)

enum class SaksbehandlingsperiodeStatus {
    UNDER_BEHANDLING,
    TIL_BESLUTNING,
    GODKJENT,
}

class SaksbehandlingsperiodeDao(private val dataSource: DataSource) {
    private val vurdertVilkårDao = VurdertVilkårDao(dataSource)

    fun lagreVilkårsvurdering(
        periode: Saksbehandlingsperiode,
        vilkårsKode: Kode,
        vurdering: JsonNode,
    ): OpprettetEllerEndret =
        vurdertVilkårDao.lagreVilkårsvurdering(
            behandling = periode,
            kode = vilkårsKode,
            vurdering = vurdering,
        )

    fun hentVurderteVilkårFor(saksbehandlingsperiodeId: UUID) = vurdertVilkårDao.hentVilkårsvurderinger(saksbehandlingsperiodeId)

    fun hentVurdertVilkårFor(
        saksbehandlingsperiodeId: UUID,
        medKode: String,
    ) = vurdertVilkårDao.hentVilkårsvurdering(saksbehandlingsperiodeId, medKode)

    fun slettVilkårsvurdering(
        saksbehandlingsperiodeId: UUID,
        kode: String,
    ) = vurdertVilkårDao.slettVilkårsvurdering(saksbehandlingsperiodeId, kode)

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
            status = SaksbehandlingsperiodeStatus.valueOf(row.string("status")),
            beslutter = row.stringOrNull("beslutter"),
        )

    fun opprettPeriode(periode: Saksbehandlingsperiode) {
        dataSource.update(
            """
            insert into saksbehandlingsperiode
                (id, spillerom_personid, opprettet, opprettet_av_nav_ident, opprettet_av_navn, fom, tom, status, beslutter)
            values
                (:id, :spillerom_personid, :opprettet, :opprettet_av_nav_ident, :opprettet_av_navn, :fom, :tom, :status, :beslutter)
            """.trimIndent(),
            "id" to periode.id,
            "spillerom_personid" to periode.spilleromPersonId,
            "opprettet" to periode.opprettet,
            "opprettet_av_nav_ident" to periode.opprettetAvNavIdent,
            "opprettet_av_navn" to periode.opprettetAvNavn,
            "fom" to periode.fom,
            "tom" to periode.tom,
            "status" to periode.status.name,
            "beslutter" to periode.beslutter,
        )
    }
}
