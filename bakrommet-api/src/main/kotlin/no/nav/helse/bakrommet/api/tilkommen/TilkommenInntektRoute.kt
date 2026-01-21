package no.nav.helse.bakrommet.api.tilkommen

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.api.PARAM_BEHANDLING_ID
import no.nav.helse.bakrommet.api.PARAM_PSEUDO_ID
import no.nav.helse.bakrommet.api.auth.bruker
import no.nav.helse.bakrommet.api.behandling.hentOgVerifiserBehandling
import no.nav.helse.bakrommet.api.behandling.hentTilkommenInntekt
import no.nav.helse.bakrommet.api.behandling.sjekkErÅpenOgTildeltSaksbehandler
import no.nav.helse.bakrommet.api.dto.tidslinje.TilkommenInntektYrkesaktivitetTypeDto.*
import no.nav.helse.bakrommet.api.dto.tilkommen.OpprettTilkommenInntektRequestDto
import no.nav.helse.bakrommet.api.errorhandling.ugyldigInput
import no.nav.helse.bakrommet.api.serde.respondJson
import no.nav.helse.bakrommet.behandling.beregning.beregnUtbetaling
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.TilkommenInntekt
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.TilkommenInntektYrkesaktivitetType
import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer

fun Route.tilkommenInntektRoute(db: DbDaoer<AlleDaoer>) {
    route("/v1/{$PARAM_PSEUDO_ID}/behandlinger/{$PARAM_BEHANDLING_ID}/tilkommeninntekt") {
        get {
            db
                .transactional {
                    val behandling = hentOgVerifiserBehandling(call)
                    tilkommenInntektRepository.finnFor(behandling.id)
                }.also { tilkomneInntekter ->
                    call.respondJson(tilkomneInntekter.map { it.tilTilkommenInntektResponseDto() })
                }
        }
        post {
            db
                .transactional {
                    val request = call.receive<OpprettTilkommenInntektRequestDto>()
                    val bruker = call.bruker()
                    val behandling =
                        hentOgVerifiserBehandling(call)
                            .sjekkErÅpenOgTildeltSaksbehandler(bruker)
                    val tilkommenInntekt =
                        TilkommenInntekt.ny(
                            behandlingId = behandling.id,
                            ident = request.ident,
                            yrkesaktivitetType =
                                when (request.yrkesaktivitetType) {
                                    VIRKSOMHET -> TilkommenInntektYrkesaktivitetType.VIRKSOMHET
                                    PRIVATPERSON -> TilkommenInntektYrkesaktivitetType.PRIVATPERSON
                                    NÆRINGSDRIVENDE -> TilkommenInntektYrkesaktivitetType.NÆRINGSDRIVENDE
                                },
                            fom = request.fom,
                            tom = request.tom,
                            inntektForPerioden = request.inntektForPerioden,
                            notatTilBeslutter = request.notatTilBeslutter,
                            ekskluderteDager = request.ekskluderteDager,
                            opprettetAvNavIdent = bruker.navIdent,
                        )
                    if (!behandling.periode.omslutter(tilkommenInntekt.periode)) {
                        ugyldigInput("Tilkommen inntekt-perioden kan ikke strekke seg ut over behandlingens periode")
                    }
                    tilkommenInntektRepository.lagre(tilkommenInntekt)
                    beregnUtbetaling(behandling, bruker)
                    tilkommenInntekt
                }.also {
                    call.respondJson(it.tilTilkommenInntektResponseDto(), status = HttpStatusCode.Created)
                }
        }

        put("/{tilkommenInntektId}") {
            val request = call.receive<OpprettTilkommenInntektRequestDto>()
            val bruker = call.bruker()
            db
                .transactional {
                    val tilkommenInntekt = hentTilkommenInntekt(call)
                    val behandling =
                        hentOgVerifiserBehandling(call)
                            .sjekkErÅpenOgTildeltSaksbehandler(bruker)
                    if (tilkommenInntekt.behandlingId != behandling.id) error("Tilkommen inntekt tilhører ikke angitt behandling")
                    if (!behandling.periode.omslutter(tilkommenInntekt.periode)) {
                        ugyldigInput("Tilkommen inntekt-perioden kan ikke strekke seg ut over behandlingens periode")
                    }
                    tilkommenInntekt.endre(
                        ident = request.ident,
                        yrkesaktivitetType =
                            when (request.yrkesaktivitetType) {
                                VIRKSOMHET -> TilkommenInntektYrkesaktivitetType.VIRKSOMHET
                                PRIVATPERSON -> TilkommenInntektYrkesaktivitetType.PRIVATPERSON
                                NÆRINGSDRIVENDE -> TilkommenInntektYrkesaktivitetType.NÆRINGSDRIVENDE
                            },
                        fom = request.fom,
                        tom = request.tom,
                        inntektForPerioden = request.inntektForPerioden,
                        notatTilBeslutter = request.notatTilBeslutter,
                        ekskluderteDager = request.ekskluderteDager,
                    )
                    tilkommenInntektRepository.lagre(tilkommenInntekt)
                    beregnUtbetaling(behandling, bruker)
                    tilkommenInntekt
                }.also {
                    call.respondJson(it.tilTilkommenInntektResponseDto())
                }
        }

        delete("/{tilkommenInntektId}") {
            val bruker = call.bruker()
            db.transactional {
                val behandling =
                    hentOgVerifiserBehandling(call)
                        .sjekkErÅpenOgTildeltSaksbehandler(bruker)
                val tilkommenInntekt = hentTilkommenInntekt(call)
                if (tilkommenInntekt.behandlingId != behandling.id) {
                    error("Tilkommen inntekt tilhører ikke angitt behandling")
                }
                tilkommenInntektRepository.slett(tilkommenInntekt.id)
                beregnUtbetaling(behandling, bruker)
            }
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
