package no.nav.helse.bakrommet.db.repository

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Session
import no.nav.helse.bakrommet.behandling.vilkaar.Vilkaarsvurdering
import no.nav.helse.bakrommet.behandling.vilkaar.VilkaarsvurderingUnderspørsmål
import no.nav.helse.bakrommet.behandling.vilkaar.Vurdering
import no.nav.helse.bakrommet.db.MedSession
import no.nav.helse.bakrommet.db.QueryRunner
import no.nav.helse.bakrommet.db.tilPgJson
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VilkårsvurderingId
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VilkårsvurderingUnderspørsmål
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VurdertVilkår
import no.nav.helse.bakrommet.repository.VilkårsvurderingRepository
import no.nav.helse.bakrommet.util.objectMapper
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
                    when (dbRecord.vurdering) {
                        Vurdering.OPPFYLT -> VurdertVilkår.Vurdering.OPPFYLT
                        Vurdering.IKKE_OPPFYLT -> VurdertVilkår.Vurdering.IKKE_OPPFYLT
                        Vurdering.IKKE_RELEVANT -> VurdertVilkår.Vurdering.IKKE_RELEVANT
                        Vurdering.SKAL_IKKE_VURDERES -> VurdertVilkår.Vurdering.SKAL_IKKE_VURDERES
                    },
                hovedspørsmål = dbRecord.hovedspørsmål,
                underspørsmål =
                    dbRecord.underspørsmål.map { dbRecordUnderspørsmål ->
                        VilkårsvurderingUnderspørsmål(
                            dbRecordUnderspørsmål.spørsmål,
                            dbRecordUnderspørsmål.svar,
                        )
                    },
                notat = dbRecord.notat,
            )
        }

    override fun lagre(vurdertVilkår: VurdertVilkår) {
        val dbRecordVurdering =
            Vilkaarsvurdering(
                vilkårskode = vurdertVilkår.id.vilkårskode.value,
                hovedspørsmål = vurdertVilkår.hovedspørsmål,
                vurdering =
                    when (vurdertVilkår.vurdering) {
                        VurdertVilkår.Vurdering.OPPFYLT -> Vurdering.OPPFYLT
                        VurdertVilkår.Vurdering.IKKE_OPPFYLT -> Vurdering.IKKE_OPPFYLT
                        VurdertVilkår.Vurdering.IKKE_RELEVANT -> Vurdering.IKKE_RELEVANT
                        VurdertVilkår.Vurdering.SKAL_IKKE_VURDERES -> Vurdering.SKAL_IKKE_VURDERES
                    },
                underspørsmål =
                    vurdertVilkår.underspørsmål.map {
                        VilkaarsvurderingUnderspørsmål(spørsmål = it.spørsmål, svar = it.svar)
                    },
                notat = vurdertVilkår.notat,
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
                "vurdering" to dbRecordVurdering.tilPgJson(),
                "vurdering_tidspunkt" to Instant.now(),
                "behandling_id" to vurdertVilkår.id.behandlingId.value,
                "kode" to vurdertVilkår.id.vilkårskode.value,
            )
    }
}
