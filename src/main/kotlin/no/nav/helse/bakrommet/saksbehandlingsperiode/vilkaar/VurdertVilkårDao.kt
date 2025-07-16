package no.nav.helse.bakrommet.saksbehandlingsperiode.vilkaar

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
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

fun VurdertVilkår.tilApiSvar(): JsonNode {
    val kopiert = vurdering.deepCopy<ObjectNode>()
    kopiert.put("kode", kode)
    return kopiert
}

class VurdertVilkårDao private constructor(private val db: QueryRunner) {
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    constructor(session: Session) : this(MedSession(session))

    fun hentVilkårsvurderinger(saksbehandlingsperiodeId: UUID): List<VurdertVilkår> =
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

    fun hentVilkårsvurdering(
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

    fun slettVilkårsvurdering(
        saksbehandlingsperiodeId: UUID,
        kode: String,
    ): Int {
        return db.update(
            """
            DELETE FROM vurdert_vilkaar
            where saksbehandlingsperiode_id = :saksbehandlingsperiode_id
            and kode = :kode
            """.trimIndent(),
            "saksbehandlingsperiode_id" to saksbehandlingsperiodeId,
            "kode" to kode,
        )
    }

    fun eksisterer(
        behandling: Saksbehandlingsperiode,
        kode: Kode,
    ): Boolean {
        return db.single(
            """
            select * from vurdert_vilkaar 
            where saksbehandlingsperiode_id = :saksbehandlingsperiode_id
            and kode = :kode
            """.trimIndent(),
            "saksbehandlingsperiode_id" to behandling.id,
            "kode" to kode.kode,
            mapper = { true },
        ) ?: false
    }

    fun oppdater(
        behandling: Saksbehandlingsperiode,
        kode: Kode,
        oppdatertVurdering: JsonNode,
    ): Int {
        return db.update(
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
    }

    fun leggTil(
        behandling: Saksbehandlingsperiode,
        kode: Kode,
        vurdering: JsonNode,
    ): Int {
        return db.update(
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
}
