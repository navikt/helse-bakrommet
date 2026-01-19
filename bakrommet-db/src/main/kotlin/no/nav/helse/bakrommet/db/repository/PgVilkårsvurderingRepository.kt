package no.nav.helse.bakrommet.db.repository

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Session
import no.nav.helse.bakrommet.behandling.vilkaar.Vilkaarsvurdering
import no.nav.helse.bakrommet.behandling.vilkaar.VilkaarsvurderingUnderspørsmål
import no.nav.helse.bakrommet.behandling.vilkaar.Vurdering
import no.nav.helse.bakrommet.db.MedSession
import no.nav.helse.bakrommet.db.QueryRunner
import no.nav.helse.bakrommet.db.tilPgJson
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Vilkårskode
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VilkårsvurderingId
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VilkårsvurderingUnderspørsmål
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VurdertVilkår
import no.nav.helse.bakrommet.objectMapper
import no.nav.helse.bakrommet.repository.VilkårsvurderingRepository
import java.time.Instant

class PgVilkårsvurderingRepository private constructor(
    private val queryRunner: QueryRunner,
) : VilkårsvurderingRepository {
    constructor(session: Session) : this(MedSession(session))

    override fun finn(vilkårsvurderingId: VilkårsvurderingId): VurdertVilkår? =
        queryRunner.single(
            sql =
                """
                select * from vurdert_vilkaar 
                where behandling_id = :behandling_id
                and kode = :kode
                """.trimIndent(),
            "behandling_id" to vilkårsvurderingId.behandlingId.value,
            "kode" to vilkårsvurderingId.vilkårskode.value,
        ) {
            val dbRecord = objectMapper.readValue<Vilkaarsvurdering>(it.string("vurdering"))
            VurdertVilkår(
                id = vilkårsvurderingId,
                vurdering =
                    VurdertVilkår.Vurdering(
                        utfall =
                            when (dbRecord.vurdering) {
                                Vurdering.OPPFYLT -> VurdertVilkår.Utfall.OPPFYLT
                                Vurdering.IKKE_OPPFYLT -> VurdertVilkår.Utfall.IKKE_OPPFYLT
                                Vurdering.IKKE_RELEVANT -> VurdertVilkår.Utfall.IKKE_RELEVANT
                                Vurdering.SKAL_IKKE_VURDERES -> VurdertVilkår.Utfall.SKAL_IKKE_VURDERES
                            },
                        underspørsmål =
                            dbRecord.underspørsmål.map { dbRecordUnderspørsmål ->
                                VilkårsvurderingUnderspørsmål(
                                    dbRecordUnderspørsmål.spørsmål,
                                    dbRecordUnderspørsmål.svar,
                                )
                            },
                        notat = dbRecord.notat,
                    ),
            )
        }

    override fun hentAlle(behandlingId: BehandlingId): List<VurdertVilkår> =
        queryRunner.list(
            sql =
                """
                select * from vurdert_vilkaar 
                where behandling_id = :behandling_id
                """.trimIndent(),
            "behandling_id" to behandlingId.value,
        ) {
            val dbRecord = objectMapper.readValue<Vilkaarsvurdering>(it.string("vurdering"))
            val vilkårskode = Vilkårskode(it.string("kode"))
            VurdertVilkår(
                id = VilkårsvurderingId(behandlingId, vilkårskode),
                vurdering =
                    VurdertVilkår.Vurdering(
                        utfall =
                            when (dbRecord.vurdering) {
                                Vurdering.OPPFYLT -> VurdertVilkår.Utfall.OPPFYLT
                                Vurdering.IKKE_OPPFYLT -> VurdertVilkår.Utfall.IKKE_OPPFYLT
                                Vurdering.IKKE_RELEVANT -> VurdertVilkår.Utfall.IKKE_RELEVANT
                                Vurdering.SKAL_IKKE_VURDERES -> VurdertVilkår.Utfall.SKAL_IKKE_VURDERES
                            },
                        underspørsmål =
                            dbRecord.underspørsmål.map { dbRecordUnderspørsmål ->
                                VilkårsvurderingUnderspørsmål(
                                    dbRecordUnderspørsmål.spørsmål,
                                    dbRecordUnderspørsmål.svar,
                                )
                            },
                        notat = dbRecord.notat,
                    ),
            )
        }

    override fun lagre(vurdertVilkår: VurdertVilkår) {
        val dbRecordUtfall =
            Vilkaarsvurdering(
                vilkårskode = vurdertVilkår.id.vilkårskode.value,
                hovedspørsmål = vurdertVilkår.id.vilkårskode.value,
                vurdering =
                    when (vurdertVilkår.vurdering.utfall) {
                        VurdertVilkår.Utfall.OPPFYLT -> Vurdering.OPPFYLT
                        VurdertVilkår.Utfall.IKKE_OPPFYLT -> Vurdering.IKKE_OPPFYLT
                        VurdertVilkår.Utfall.IKKE_RELEVANT -> Vurdering.IKKE_RELEVANT
                        VurdertVilkår.Utfall.SKAL_IKKE_VURDERES -> Vurdering.SKAL_IKKE_VURDERES
                    },
                underspørsmål =
                    vurdertVilkår.vurdering.underspørsmål.map {
                        VilkaarsvurderingUnderspørsmål(spørsmål = it.spørsmål, svar = it.svar)
                    },
                notat = vurdertVilkår.vurdering.notat,
            )
        queryRunner
            .update(
                """
                insert into vurdert_vilkaar
                 (vurdering, vurdering_tidspunkt, behandling_id, kode)
                values (:vurdering, :vurdering_tidspunkt, :behandling_id, :kode)
                on conflict (behandling_id, kode)
                do update
                set 
                    vurdering = excluded.vurdering,
                    vurdering_tidspunkt = excluded.vurdering_tidspunkt
                    
                """.trimIndent(),
                "vurdering" to dbRecordUtfall.tilPgJson(),
                "vurdering_tidspunkt" to Instant.now(),
                "behandling_id" to vurdertVilkår.id.behandlingId.value,
                "kode" to vurdertVilkår.id.vilkårskode.value,
            )
    }

    override fun slett(vilkårsvurderingId: VilkårsvurderingId) {
        queryRunner.update(
            sql =
                """
                delete from vurdert_vilkaar 
                where behandling_id = :behandling_id
                and kode = :kode
                """.trimIndent(),
            "behandling_id" to vilkårsvurderingId.behandlingId.value,
            "kode" to vilkårsvurderingId.vilkårskode.value,
        )
    }
}
