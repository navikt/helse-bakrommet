package no.nav.helse.bakrommet.saksbehandlingsperiode.vilkaar

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.util.*
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

data class VurdertVilkår(
    val kode: String,
    val vurdering: JsonNode,
)

enum class OpprettetEllerEndret {
    OPPRETTET,
    ENDRET,
}

class VurdertVilkårDao(private val dataSource: DataSource) {
    fun hentVilkårsvurderinger(saksbehandlingsperiodeId: UUID): List<VurdertVilkår> =
        dataSource.list(
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
        dataSource.single(
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
        return sessionOf(dataSource, strict = true).use { session ->
            session.run(
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
    }

    fun lagreVilkårsvurdering(
        behandling: Saksbehandlingsperiode,
        kode: Kode,
        vurdering: JsonNode,
    ): OpprettetEllerEndret {
        sessionOf(dataSource, strict = true).use { session ->
            session.transaction { tx ->
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
    }
}
