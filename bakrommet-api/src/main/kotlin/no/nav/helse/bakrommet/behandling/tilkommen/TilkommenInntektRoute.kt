package no.nav.helse.bakrommet.behandling.tilkommen

import io.ktor.server.request.receive
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.PARAM_PERIODEUUID
import no.nav.helse.bakrommet.PARAM_PERSONID
import no.nav.helse.bakrommet.PARAM_TILKOMMENINNTEKT_ID
import no.nav.helse.bakrommet.auth.saksbehandler
import no.nav.helse.bakrommet.behandling.periodeReferanse
import no.nav.helse.bakrommet.serde.respondJson
import no.nav.helse.bakrommet.util.somGyldigUUID
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

fun RoutingCall.tilkommenInntektReferanse() =
    TilkommenInntektReferanse(
        behandling = periodeReferanse(),
        tilkommenInntektId = parameters[PARAM_TILKOMMENINNTEKT_ID].somGyldigUUID(),
    )

internal fun Route.tilkommenInntektRoute(service: TilkommenInntektService) {
    route("/v1/{$PARAM_PERSONID}/saksbehandlingsperioder/{$PARAM_PERIODEUUID}/tilkommeninntekt") {
        get {
            val ref = call.periodeReferanse()
            service.hentTilkommenInntekt(ref).also { tilkommenInntektDbRecords ->
                call.respondJson(tilkommenInntektDbRecords.map { it.tilTilkommenInntektResponse() })
            }
        }
        post {
            val ref = call.periodeReferanse()
            val request = call.receive<OpprettTilkommenInntektRequest>()
            val nyTilkommenInntekt =
                service.lagreTilkommenInntekt(
                    ref = ref,
                    tilkommenInntekt = request.tilTilkommenInntekt(),
                    saksbehandler = call.saksbehandler(),
                )
            call.respondJson(nyTilkommenInntekt.tilTilkommenInntektResponse())
        }

        put("/{tilkommenInntektId}") {
            val ref = call.tilkommenInntektReferanse()
            val request = call.receive<OpprettTilkommenInntektRequest>()

            service
                .endreTilkommenInntekt(
                    ref = ref,
                    tilkommenInntekt = request.tilTilkommenInntekt(),
                    saksbehandler = call.saksbehandler(),
                ).also {
                    call.respondJson(it)
                }
        }

        delete("/{tilkommenInntektId}") {
            val ref = call.tilkommenInntektReferanse()

            service.slettTilkommenInntekt(
                ref = ref,
                saksbehandler = call.saksbehandler(),
            )
        }
    }
}

private fun OpprettTilkommenInntektRequest.tilTilkommenInntekt(): TilkommenInntekt =
    TilkommenInntekt(
        ident = this.ident,
        yrkesaktivitetType = this.yrkesaktivitetType,
        fom = this.fom,
        tom = this.tom,
        inntektForPerioden = this.inntektForPerioden,
        notatTilBeslutter = this.notatTilBeslutter,
        ekskluderteDager = this.ekskluderteDager,
    )

data class OpprettTilkommenInntektRequest(
    val ident: String,
    val yrkesaktivitetType: TilkommenInntektYrkesaktivitetType,
    val fom: LocalDate,
    val tom: LocalDate,
    val inntektForPerioden: BigDecimal,
    val notatTilBeslutter: String,
    val ekskluderteDager: List<LocalDate>,
)

data class TilkommenInntektResponse(
    val id: UUID,
    val ident: String,
    val yrkesaktivitetType: TilkommenInntektYrkesaktivitetType,
    val fom: LocalDate,
    val tom: LocalDate,
    val inntektForPerioden: BigDecimal,
    val notatTilBeslutter: String,
    val ekskluderteDager: List<LocalDate>,
    val opprettet: OffsetDateTime,
    val opprettetAvNavIdent: String,
)

private fun TilkommenInntektDbRecord.tilTilkommenInntektResponse(): TilkommenInntektResponse =
    TilkommenInntektResponse(
        id = this.id,
        ident = this.tilkommenInntekt.ident,
        yrkesaktivitetType = this.tilkommenInntekt.yrkesaktivitetType,
        fom = this.tilkommenInntekt.fom,
        tom = this.tilkommenInntekt.tom,
        inntektForPerioden = this.tilkommenInntekt.inntektForPerioden,
        notatTilBeslutter = this.tilkommenInntekt.notatTilBeslutter,
        ekskluderteDager = this.tilkommenInntekt.ekskluderteDager,
        opprettet = this.opprettet,
        opprettetAvNavIdent = this.opprettetAvNavIdent,
    )
