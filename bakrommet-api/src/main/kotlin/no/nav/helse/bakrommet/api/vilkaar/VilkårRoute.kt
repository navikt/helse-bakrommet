package no.nav.helse.bakrommet.api.vilkaar

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.api.PARAM_BEHANDLING_ID
import no.nav.helse.bakrommet.api.PARAM_PSEUDO_ID
import no.nav.helse.bakrommet.api.behandlingId
import no.nav.helse.bakrommet.api.dto.vilkaar.OppdaterVilkaarsvurderingResponseDto
import no.nav.helse.bakrommet.api.dto.vilkaar.VilkaarsvurderingRequestDto
import no.nav.helse.bakrommet.api.dto.vilkaar.VurderingDto
import no.nav.helse.bakrommet.api.pseudoId
import no.nav.helse.bakrommet.api.serde.respondJson
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Vilkårskode
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VilkårsvurderingId
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VilkårsvurderingUnderspørsmål
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VurdertVilkår
import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer

fun Route.vilkårRoute(
    db: DbDaoer<AlleDaoer>,
) {
    route("/v1/{$PARAM_PSEUDO_ID}/behandlinger/{$PARAM_BEHANDLING_ID}/vilkaarsvurdering") {
        get {
            val behandlingId = call.behandlingId()
            val pseudoId = call.pseudoId()

            db.transactional {
                val behandling = behandlingRepository.hent(behandlingId)
                val naturligIdent = personPseudoIdDao.hentNaturligIdent(pseudoId)
                if (!behandling.gjelder(naturligIdent)) {
                    throw IllegalArgumentException("Behandling ${behandlingId.value} gjelder ikke personen med pseudoId=${pseudoId.value}")
                }

                val vurderteVilkår =
                    vilkårsvurderingRepository.hentAlle(behandlingId).map {
                        it.skapVilkaarsvurderingDto()
                    }
                call.respondJson(vurderteVilkår)
            }
        }
    }

    route("/v1/{$PARAM_PSEUDO_ID}/behandlinger/{$PARAM_BEHANDLING_ID}/vilkaarsvurdering/{hovedspørsmål}") {
        put {
            val request = call.receive<VilkaarsvurderingRequestDto>()

            val behandlingId = call.behandlingId()
            val pseudoId = call.pseudoId()

            db.transactional {
                val behandling = behandlingRepository.hent(behandlingId)
                val naturligIdent = personPseudoIdDao.hentNaturligIdent(pseudoId)
                if (!behandling.gjelder(naturligIdent)) {
                    throw IllegalArgumentException("Behandling ${behandlingId.value} gjelder ikke personen med pseudoId=${pseudoId.value}")
                }
                val vilkårskode = Vilkårskode(call.parameters["hovedspørsmål"]!!)

                val vilkårsvurderingId =
                    VilkårsvurderingId(
                        behandlingId,
                        vilkårskode,
                    )

                val vurdering =
                    VurdertVilkår.Vurdering(
                        underspørsmål =
                            request.underspørsmål.map {
                                VilkårsvurderingUnderspørsmål(
                                    spørsmål = it.spørsmål,
                                    svar = it.svar,
                                )
                            },
                        utfall =
                            when (request.vurdering) {
                                VurderingDto.OPPFYLT -> VurdertVilkår.Utfall.OPPFYLT
                                VurderingDto.IKKE_OPPFYLT -> VurdertVilkår.Utfall.IKKE_OPPFYLT
                                VurderingDto.IKKE_RELEVANT -> VurdertVilkår.Utfall.IKKE_RELEVANT
                                VurderingDto.SKAL_IKKE_VURDERES -> VurdertVilkår.Utfall.SKAL_IKKE_VURDERES
                            },
                        notat = request.notat,
                    )

                val eksisterendeVurdertVilkår = vilkårsvurderingRepository.finn(vilkårsvurderingId)
                val (vurdertVilkår, httpStatus) =
                    if (eksisterendeVurdertVilkår != null) {
                        eksisterendeVurdertVilkår.nyVurdering(vurdering)
                        eksisterendeVurdertVilkår to HttpStatusCode.OK
                    } else {
                        VurdertVilkår.ny(vilkårsvurderingId, vurdering) to HttpStatusCode.Created
                    }

                val response =
                    OppdaterVilkaarsvurderingResponseDto(
                        vilkaarsvurderingDto = vurdertVilkår.skapVilkaarsvurderingDto(),
                        invalidations = emptyList(),
                    )
                vilkårsvurderingRepository.lagre(vurdertVilkår)
                call.respondJson(
                    response,
                    status = httpStatus,
                )
            }
        }

        delete {
            val behandlingId = call.behandlingId()
            val pseudoId = call.pseudoId()
            val vilkårskode = Vilkårskode(call.parameters["hovedspørsmål"]!!)

            db.transactional {
                val behandling = behandlingRepository.hent(behandlingId)
                val naturligIdent = personPseudoIdDao.hentNaturligIdent(pseudoId)
                if (!behandling.gjelder(naturligIdent)) {
                    throw IllegalArgumentException("Behandling ${behandlingId.value} gjelder ikke personen med pseudoId=${pseudoId.value}")
                }

                val vilkårsvurderingId =
                    VilkårsvurderingId(
                        behandlingId,
                        vilkårskode,
                    )

                vilkårsvurderingRepository.slett(vilkårsvurderingId)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
