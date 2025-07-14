package no.nav.helse.bakrommet.saksbehandlingsperiode.vilkaar

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import kotliquery.Session
import kotliquery.queryOf
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

enum class OpprettetEllerEndret {
    OPPRETTET,
    ENDRET,
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
        return db.run(
            queryOf(
                """
                DELETE FROM vurdert_vilkaar
                where saksbehandlingsperiode_id = :saksbehandlingsperiode_id
                and kode = :kode
                """.trimIndent(),
                mapOf(
                    "saksbehandlingsperiode_id" to saksbehandlingsperiodeId,
                    "kode" to kode,
                ),
            ).asUpdate,
        )
    }

    fun lagreVilkårsvurdering(
        behandling: Saksbehandlingsperiode,
        kode: Kode,
        vurdering: JsonNode,
    ): OpprettetEllerEndret {
        require(db is MedSession) { "Denne operasjonen må kjøres i en transaksjon" }
        val tx = db // TODO: Flytt denne transaksjonslogikken ut av DAOen (?)

        val finnesFraFør =
            tx.run(
                queryOf(
                    """
                    select * from vurdert_vilkaar 
                    where saksbehandlingsperiode_id = :saksbehandlingsperiode_id
                    and kode = :kode
                    """.trimIndent(),
                    mapOf(
                        "saksbehandlingsperiode_id" to behandling.id,
                        "kode" to kode.kode,
                    ),
                ).map { true }.asSingle,
            ) ?: false

        if (finnesFraFør) {
            tx.run(
                queryOf(
                    """
                    update vurdert_vilkaar 
                    set vurdering = :vurdering,
                    vurdering_tidspunkt = :vurdering_tidspunkt
                    where saksbehandlingsperiode_id = :saksbehandlingsperiode_id
                    and kode = :kode 
                    """.trimIndent(),
                    mapOf(
                        "vurdering" to vurdering.serialisertTilString(),
                        "vurdering_tidspunkt" to Instant.now(),
                        "saksbehandlingsperiode_id" to behandling.id,
                        "kode" to kode.kode,
                    ),
                ).asUpdate,
            )
            return OpprettetEllerEndret.ENDRET
        } else {
            tx.run(
                queryOf(
                    """
                    insert into vurdert_vilkaar
                     (vurdering, vurdering_tidspunkt, saksbehandlingsperiode_id, kode)
                    values (:vurdering, :vurdering_tidspunkt, :saksbehandlingsperiode_id, :kode) 
                    """.trimIndent(),
                    mapOf(
                        "vurdering" to vurdering.serialisertTilString(),
                        "vurdering_tidspunkt" to Instant.now(),
                        "saksbehandlingsperiode_id" to behandling.id,
                        "kode" to kode.kode,
                    ),
                ).asUpdate,
            )
            return OpprettetEllerEndret.OPPRETTET
        }
    }
}
