package no.nav.helse.bakrommet.db.dao

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Session
import no.nav.helse.bakrommet.behandling.BehandlingDbRecord
import no.nav.helse.bakrommet.behandling.vilkaar.Kode
import no.nav.helse.bakrommet.behandling.vilkaar.LegacyVurdertVilkår
import no.nav.helse.bakrommet.behandling.vilkaar.Vilkaarsvurdering
import no.nav.helse.bakrommet.behandling.vilkaar.VurdertVilkårDao
import no.nav.helse.bakrommet.db.MedDataSource
import no.nav.helse.bakrommet.db.MedSession
import no.nav.helse.bakrommet.db.QueryRunner
import no.nav.helse.bakrommet.db.tilPgJson
import no.nav.helse.bakrommet.errorhandling.KunneIkkeOppdatereDbException
import no.nav.helse.bakrommet.util.objectMapper
import java.time.Instant
import java.util.*
import javax.sql.DataSource

private val verifiserOppdatert: (Int) -> Unit = {
    if (it == 0) {
        throw KunneIkkeOppdatereDbException("Vurder vilkår kunne ikke oppdateres")
    }
}

private const val AND_ER_UNDER_BEHANDLING = "AND (select status from behandling where behandling.id = vurdert_vilkaar.behandling_id) = '${STATUS_UNDER_BEHANDLING_STR}'"
private const val WHERE_ER_UNDER_BEHANDLING_FOR_INSERT = "WHERE EXISTS (select 1 from behandling where behandling.id = :behandling_id and status = '${STATUS_UNDER_BEHANDLING_STR}')"

class VurdertVilkårDaoPg private constructor(
    private val db: QueryRunner,
) : `VurdertVilkårDao` {
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    constructor(session: Session) : this(MedSession(session))

    override fun hentVilkårsvurderinger(behandlingId: UUID): List<LegacyVurdertVilkår> =
        db.list(
            sql =
                """
                select * from vurdert_vilkaar 
                where behandling_id = :behandling_id
                """.trimIndent(),
            "behandling_id" to behandlingId,
        ) {
            val vurderingJson = it.string("vurdering")
            val vurdering: Vilkaarsvurdering = objectMapper.readValue(vurderingJson)
            LegacyVurdertVilkår(
                kode = it.string("kode"),
                vurdering = vurdering,
            )
        }

    override fun leggTil(
        behandlingDbRecord: BehandlingDbRecord,
        kode: Kode,
        vurdering: Vilkaarsvurdering,
    ): Int =
        db
            .update(
                """
                insert into vurdert_vilkaar
                 (vurdering, vurdering_tidspunkt, behandling_id, kode)
                select :vurdering, :vurdering_tidspunkt, :behandling_id, :kode
                 $WHERE_ER_UNDER_BEHANDLING_FOR_INSERT
                """.trimIndent(),
                "vurdering" to vurdering.tilPgJson(),
                "vurdering_tidspunkt" to Instant.now(),
                "behandling_id" to behandlingDbRecord.id,
                "kode" to kode.kode,
            ).also(verifiserOppdatert)
}
