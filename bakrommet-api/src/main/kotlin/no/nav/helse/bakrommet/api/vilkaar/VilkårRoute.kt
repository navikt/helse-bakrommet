package no.nav.helse.bakrommet.api.vilkaar

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.api.PARAM_BEHANDLING_ID
import no.nav.helse.bakrommet.api.PARAM_PSEUDO_ID
import no.nav.helse.bakrommet.api.auth.saksbehandler
import no.nav.helse.bakrommet.api.behandlingId
import no.nav.helse.bakrommet.api.dto.vilkaar.OppdaterVilkaarsvurderingResponseDto
import no.nav.helse.bakrommet.api.dto.vilkaar.VilkaarsvurderingRequestDto
import no.nav.helse.bakrommet.api.dto.vilkaar.VurderingDto
import no.nav.helse.bakrommet.api.periodeReferanse
import no.nav.helse.bakrommet.api.pseudoId
import no.nav.helse.bakrommet.api.serde.respondJson
import no.nav.helse.bakrommet.behandling.vilkaar.Kode
import no.nav.helse.bakrommet.behandling.vilkaar.VilkårServiceOld
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Vilkårskode
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VilkårsvurderingId
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VilkårsvurderingUnderspørsmål
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VurdertVilkår
import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.person.PersonService

fun Route.vilkårRoute(
    service: VilkårServiceOld,
    personService: PersonService,
    db: DbDaoer<AlleDaoer>,
) {
    route("/v1/{$PARAM_PSEUDO_ID}/behandlinger/{$PARAM_BEHANDLING_ID}/vilkaarsvurdering") {
        get {
            val vurderteVilkår =
                service.hentVilkårsvurderingerFor(call.periodeReferanse(personService)).map {
                    it.tilVilkaarsvurderingDto()
                }
            call.respondJson(vurderteVilkår)
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

                val eksisterendeVurdertVilkår = vilkårsvurderingRepository.finn(vilkårsvurderingId)
                if (eksisterendeVurdertVilkår != null) {
                    eksisterendeVurdertVilkår.nyVurdering(
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
                        ),
                    )

                    val response =
                        OppdaterVilkaarsvurderingResponseDto(
                            vilkaarsvurderingDto = eksisterendeVurdertVilkår.skapVilkaarsvurderingDto(),
                            invalidations = emptyList(),
                        )
                    vilkårsvurderingRepository.lagre(eksisterendeVurdertVilkår)
                    call.respondJson(
                        response,
                        status = HttpStatusCode.OK,
                    )
                } else {
                    val vurdertVilkår =
                        VurdertVilkår.ny(
                            vilkårsvurderingId,
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
                            ),
                        )

                    val response =
                        OppdaterVilkaarsvurderingResponseDto(
                            vilkaarsvurderingDto = vurdertVilkår.skapVilkaarsvurderingDto(),
                            invalidations = emptyList(),
                        )
                    vilkårsvurderingRepository.lagre(vurdertVilkår)
                    call.respondJson(
                        response,
                        status = HttpStatusCode.Created,
                    )
                }
            }
        }

        delete {
            val bleFunnetOgSlettet =
                service.slettVilkårsvurdering(
                    ref = call.periodeReferanse(personService),
                    vilkårsKode = Kode(call.parameters["hovedspørsmål"]!!),
                    saksbehandler = call.saksbehandler(),
                )
            if (bleFunnetOgSlettet) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}
