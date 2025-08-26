package no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.PARAM_PERIODEUUID
import no.nav.helse.bakrommet.PARAM_PERSONID
import no.nav.helse.bakrommet.auth.saksbehandler
import no.nav.helse.bakrommet.saksbehandlingsperiode.periodeReferanse
import no.nav.helse.bakrommet.util.serialisertTilString
import java.time.LocalDate
import java.util.*

data class Inntekt(
    val yrkesaktivitetId: UUID,
    // Beløp i øre
    val beløpPerMånedØre: Long,
    val kilde: Inntektskilde,
    val refusjon: List<Refusjonsperiode> = emptyList(),
)

data class Refusjonsperiode(
    val fom: LocalDate,
    val tom: LocalDate,
    // Beløp i øre
    val beløpØre: Long,
)

enum class Inntektskilde {
    AINNTEKT,
    INNTEKTSMELDING,
    PENSJONSGIVENDE_INNTEKT,
    SKJONNSFASTSETTELSE,
}

data class SykepengegrunnlagRequest(
    val inntekter: List<Inntekt>,
    val begrunnelse: String? = null,
)

data class SykepengegrunnlagResponse(
    val id: UUID,
    val saksbehandlingsperiodeId: UUID,
    val inntekter: List<Inntekt>,
    val totalInntektØre: Long,
    val grunnbeløpØre: Long,
    val grunnbeløp6GØre: Long,
    val begrensetTil6G: Boolean,
    // Endelig grunnlag i øre
    val sykepengegrunnlagØre: Long,
    val begrunnelse: String? = null,
    // New field for the virkningstidspunkt of the Grunnbeløp
    val grunnbeløpVirkningstidspunkt: LocalDate,
    val opprettet: String,
    val opprettetAv: String,
    val sistOppdatert: String,
)

internal fun Route.sykepengegrunnlagRoute(service: SykepengegrunnlagService) {
    route("/v1/{$PARAM_PERSONID}/saksbehandlingsperioder/{$PARAM_PERIODEUUID}/sykepengegrunnlag") {
        /** Hent eksisterende sykepengegrunnlag */
        get {
            val grunnlag = service.hentSykepengegrunnlag(call.periodeReferanse())
            call.respondText(
                grunnlag?.serialisertTilString() ?: "null",
                ContentType.Application.Json,
                HttpStatusCode.OK,
            )
        }

        /** Sett sykepengegrunnlag (opprett eller oppdater) */
        put {
            val request = call.receive<SykepengegrunnlagRequest>()
            val grunnlag =
                service.settSykepengegrunnlag(
                    call.periodeReferanse(),
                    request,
                    call.saksbehandler(),
                )
            call.respondText(
                grunnlag.serialisertTilString(),
                ContentType.Application.Json,
                HttpStatusCode.OK,
            )
        }

        /** Slett sykepengegrunnlag */
        delete {
            service.slettSykepengegrunnlag(call.periodeReferanse())
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
