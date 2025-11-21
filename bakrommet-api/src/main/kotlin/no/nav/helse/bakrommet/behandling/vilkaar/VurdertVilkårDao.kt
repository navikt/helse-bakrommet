package no.nav.helse.bakrommet.behandling.vilkaar

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.Session
import no.nav.helse.bakrommet.behandling.Behandling
import no.nav.helse.bakrommet.behandling.STATUS_UNDER_BEHANDLING_STR
import no.nav.helse.bakrommet.errorhandling.KunneIkkeOppdatereDbException
import no.nav.helse.bakrommet.infrastruktur.db.MedDataSource
import no.nav.helse.bakrommet.infrastruktur.db.MedSession
import no.nav.helse.bakrommet.infrastruktur.db.QueryRunner
import no.nav.helse.bakrommet.util.*
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

data class VurdertVilkår(
    val kode: String,
    val vurdering: JsonNode,
)

interface VurdertVilkårDao {
    fun hentVilkårsvurderinger(saksbehandlingsperiodeId: UUID): List<VurdertVilkår>

    fun hentVilkårsvurdering(
        saksbehandlingsperiodeId: UUID,
        kode: String,
    ): VurdertVilkår?

    fun slettVilkårsvurdering(
        saksbehandlingsperiodeId: UUID,
        kode: String,
    ): Int

    fun eksisterer(
        behandling: Behandling,
        kode: Kode,
    ): Boolean

    fun oppdater(
        behandling: Behandling,
        kode: Kode,
        oppdatertVurdering: JsonNode,
    ): Int

    fun leggTil(
        behandling: Behandling,
        kode: Kode,
        vurdering: JsonNode,
    ): Int
}

private val verifiserOppdatert: (Int) -> Unit = {
    if (it == 0) {
        throw KunneIkkeOppdatereDbException("Vurder vilkår kunne ikke oppdateres")
    }
}

private const val AND_ER_UNDER_BEHANDLING = "AND (select status from behandling where behandling.id = vurdert_vilkaar.behandling_id) = '$STATUS_UNDER_BEHANDLING_STR'"
private const val WHERE_ER_UNDER_BEHANDLING_FOR_INSERT = "WHERE EXISTS (select 1 from behandling where behandling.id = :behandling_id and status = '$STATUS_UNDER_BEHANDLING_STR')"

class VurdertVilkårDaoPg private constructor(
    private val db: QueryRunner,
) : VurdertVilkårDao {
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    constructor(session: Session) : this(MedSession(session))

    override fun hentVilkårsvurderinger(saksbehandlingsperiodeId: UUID): List<VurdertVilkår> =
        db.list(
            sql =
                """
                select * from vurdert_vilkaar 
                where behandling_id = :behandling_id
                """.trimIndent(),
            "behandling_id" to saksbehandlingsperiodeId,
        ) {
            VurdertVilkår(
                kode = it.string("kode"),
                vurdering = it.string("vurdering").asJsonNode(),
            )
        }

    override fun hentVilkårsvurdering(
        saksbehandlingsperiodeId: UUID,
        kode: String,
    ): VurdertVilkår? =
        db.single(
            sql =
                """
                select * from vurdert_vilkaar 
                where behandling_id = :behandling_id
                and kode = :kode
                """.trimIndent(),
            "behandling_id" to saksbehandlingsperiodeId,
            "kode" to kode,
        ) {
            VurdertVilkår(
                kode = it.string("kode"),
                vurdering = it.string("vurdering").asJsonNode(),
            )
        }

    override fun slettVilkårsvurdering(
        saksbehandlingsperiodeId: UUID,
        kode: String,
    ): Int =
        db
            .update(
                """
                DELETE FROM vurdert_vilkaar
                where behandling_id = :behandling_id
                and kode = :kode
                $AND_ER_UNDER_BEHANDLING
                """.trimIndent(),
                "behandling_id" to saksbehandlingsperiodeId,
                "kode" to kode,
            ).also(verifiserOppdatert)

    override fun eksisterer(
        behandling: Behandling,
        kode: Kode,
    ): Boolean =
        db.single(
            """
            select * from vurdert_vilkaar 
            where behandling_id = :behandling_id
            and kode = :kode
            """.trimIndent(),
            "behandling_id" to behandling.id,
            "kode" to kode.kode,
            mapper = { true },
        ) ?: false

    override fun oppdater(
        behandling: Behandling,
        kode: Kode,
        oppdatertVurdering: JsonNode,
    ): Int =
        db
            .update(
                """
                update vurdert_vilkaar 
                set vurdering = :vurdering,
                vurdering_tidspunkt = :vurdering_tidspunkt
                where behandling_id = :behandling_id
                and kode = :kode 
                $AND_ER_UNDER_BEHANDLING
                """.trimIndent(),
                "vurdering" to oppdatertVurdering.serialisertTilString(),
                "vurdering_tidspunkt" to Instant.now(),
                "behandling_id" to behandling.id,
                "kode" to kode.kode,
            ).also(verifiserOppdatert)

    override fun leggTil(
        behandling: Behandling,
        kode: Kode,
        vurdering: JsonNode,
    ): Int =
        db
            .update(
                """
                insert into vurdert_vilkaar
                 (vurdering, vurdering_tidspunkt, behandling_id, kode)
                select :vurdering, :vurdering_tidspunkt, :behandling_id, :kode
                 $WHERE_ER_UNDER_BEHANDLING_FOR_INSERT
                """.trimIndent(),
                "vurdering" to vurdering.serialisertTilString(),
                "vurdering_tidspunkt" to Instant.now(),
                "behandling_id" to behandling.id,
                "kode" to kode.kode,
            ).also(verifiserOppdatert)
}
