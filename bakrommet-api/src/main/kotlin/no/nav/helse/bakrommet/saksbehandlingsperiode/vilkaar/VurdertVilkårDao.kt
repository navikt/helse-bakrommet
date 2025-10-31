package no.nav.helse.bakrommet.saksbehandlingsperiode.vilkaar

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.Session
import no.nav.helse.bakrommet.infrastruktur.db.MedDataSource
import no.nav.helse.bakrommet.infrastruktur.db.MedSession
import no.nav.helse.bakrommet.infrastruktur.db.QueryRunner
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.util.*
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

data class VurdertVilkår(
    val kode: String,
    val vurdering: JsonNode,
)

interface VurdertVilkårDao {
    suspend fun hentVilkårsvurderinger(saksbehandlingsperiodeId: UUID): List<VurdertVilkår>

    suspend fun hentVilkårsvurdering(
        saksbehandlingsperiodeId: UUID,
        kode: String,
    ): VurdertVilkår?

    suspend fun slettVilkårsvurdering(
        saksbehandlingsperiodeId: UUID,
        kode: String,
    ): Int

    suspend fun eksisterer(
        behandling: Saksbehandlingsperiode,
        kode: Kode,
    ): Boolean

    suspend fun oppdater(
        behandling: Saksbehandlingsperiode,
        kode: Kode,
        oppdatertVurdering: JsonNode,
    ): Int

    suspend fun leggTil(
        behandling: Saksbehandlingsperiode,
        kode: Kode,
        vurdering: JsonNode,
    ): Int
}

class VurdertVilkårDaoPg private constructor(
    private val db: QueryRunner,
) : VurdertVilkårDao {
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    constructor(session: Session) : this(MedSession(session))

    override suspend fun hentVilkårsvurderinger(saksbehandlingsperiodeId: UUID): List<VurdertVilkår> =
        db.list(
            sql =
                """
                select * from vurdert_vilkaar 
                where saksbehandlingsperiode_id = :saksbehandlingsperiode_id
                """.trimIndent(),
            "saksbehandlingsperiode_id" to saksbehandlingsperiodeId,
        ) {
            VurdertVilkår(
                kode = it.string("kode"),
                vurdering = it.string("vurdering").asJsonNode(),
            )
        }

    override suspend fun hentVilkårsvurdering(
        saksbehandlingsperiodeId: UUID,
        kode: String,
    ): VurdertVilkår? =
        db.single(
            sql =
                """
                select * from vurdert_vilkaar 
                where saksbehandlingsperiode_id = :saksbehandlingsperiode_id
                and kode = :kode
                """.trimIndent(),
            "saksbehandlingsperiode_id" to saksbehandlingsperiodeId,
            "kode" to kode,
        ) {
            VurdertVilkår(
                kode = it.string("kode"),
                vurdering = it.string("vurdering").asJsonNode(),
            )
        }

    override suspend fun slettVilkårsvurdering(
        saksbehandlingsperiodeId: UUID,
        kode: String,
    ): Int =
        db.update(
            """
            DELETE FROM vurdert_vilkaar
            where saksbehandlingsperiode_id = :saksbehandlingsperiode_id
            and kode = :kode
            """.trimIndent(),
            "saksbehandlingsperiode_id" to saksbehandlingsperiodeId,
            "kode" to kode,
        )

    override suspend fun eksisterer(
        behandling: Saksbehandlingsperiode,
        kode: Kode,
    ): Boolean =
        db.single(
            """
            select * from vurdert_vilkaar 
            where saksbehandlingsperiode_id = :saksbehandlingsperiode_id
            and kode = :kode
            """.trimIndent(),
            "saksbehandlingsperiode_id" to behandling.id,
            "kode" to kode.kode,
            mapper = { true },
        ) ?: false

    override suspend fun oppdater(
        behandling: Saksbehandlingsperiode,
        kode: Kode,
        oppdatertVurdering: JsonNode,
    ): Int =
        db.update(
            """
            update vurdert_vilkaar 
            set vurdering = :vurdering,
            vurdering_tidspunkt = :vurdering_tidspunkt
            where saksbehandlingsperiode_id = :saksbehandlingsperiode_id
            and kode = :kode 
            """.trimIndent(),
            "vurdering" to oppdatertVurdering.serialisertTilString(),
            "vurdering_tidspunkt" to Instant.now(),
            "saksbehandlingsperiode_id" to behandling.id,
            "kode" to kode.kode,
        )

    override suspend fun leggTil(
        behandling: Saksbehandlingsperiode,
        kode: Kode,
        vurdering: JsonNode,
    ): Int =
        db.update(
            """
            insert into vurdert_vilkaar
             (vurdering, vurdering_tidspunkt, saksbehandlingsperiode_id, kode)
            values (:vurdering, :vurdering_tidspunkt, :saksbehandlingsperiode_id, :kode) 
            """.trimIndent(),
            "vurdering" to vurdering.serialisertTilString(),
            "vurdering_tidspunkt" to Instant.now(),
            "saksbehandlingsperiode_id" to behandling.id,
            "kode" to kode.kode,
        )
}
