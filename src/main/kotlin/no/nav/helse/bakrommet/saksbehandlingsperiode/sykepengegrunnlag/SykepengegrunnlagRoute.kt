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
import java.util.*

data class FaktiskInntekt(
    val id: UUID? = null, // null ved opprettelse
    val inntektsforholdId: UUID,
    val beløpPerMånedØre: Long, // Beløp i øre
    val kilde: Inntektskilde,
    val erSkjønnsfastsatt: Boolean = false,
    val skjønnsfastsettelseBegrunnelse: String? = null,
    val refusjon: Refusjonsforhold? = null,
    val opprettetAv: String? = null, // Settes av service
)

data class Refusjonsforhold(
    val refusjonsbeløpPerMånedØre: Long, // Beløp i øre
    val refusjonsgrad: Int, // 0-100
)

enum class Inntektskilde {
    AINNTEKT,
    SAKSBEHANDLER,
    SKJONNSFASTSETTELSE,
}

data class SykepengegrunnlagRequest(
    val faktiskeInntekter: List<FaktiskInntekt>,
    val begrunnelse: String? = null,
)

data class SykepengegrunnlagResponse(
    val id: UUID,
    val saksbehandlingsperiodeId: UUID,
    val faktiskeInntekter: List<FaktiskInntekt>,
    val totalInntektØre: Long, // Årsinntekt i øre
    val grunnbeløp6GØre: Long, // 6G i øre
    val begrensetTil6G: Boolean,
    val sykepengegrunnlagØre: Long, // Endelig grunnlag i øre
    val begrunnelse: String? = null,
    val opprettet: String,
    val opprettetAv: String,
    val sistOppdatert: String,
    val versjon: Int,
)

internal fun Route.sykepengegrunnlagRoute(service: SykepengegrunnlagService) {
    route("/v1/{$PARAM_PERSONID}/saksbehandlingsperioder/{$PARAM_PERIODEUUID}/sykepengegrunnlag") {
        /** Hent eksisterende sykepengegrunnlag */
        get {
            val grunnlag = service.hentSykepengegrunnlag(call.periodeReferanse())
            if (grunnlag != null) {
                call.respondText(
                    grunnlag.serialisertTilString(),
                    ContentType.Application.Json,
                    HttpStatusCode.OK,
                )
            } else {
                call.respond(HttpStatusCode.NotFound, "Ingen sykepengegrunnlag funnet for periode")
            }
        }

        /** Opprett nytt sykepengegrunnlag */
        post {
            val request = call.receive<SykepengegrunnlagRequest>()
            val nyttGrunnlag =
                service.opprettSykepengegrunnlag(
                    call.periodeReferanse(),
                    request,
                    call.saksbehandler(),
                )
            call.respondText(
                nyttGrunnlag.serialisertTilString(),
                ContentType.Application.Json,
                HttpStatusCode.Created,
            )
        }

        /** Oppdater eksisterende sykepengegrunnlag */
        put {
            val request = call.receive<SykepengegrunnlagRequest>()
            val oppdatertGrunnlag =
                service.oppdaterSykepengegrunnlag(
                    call.periodeReferanse(),
                    request,
                    call.saksbehandler(),
                )
            call.respondText(
                oppdatertGrunnlag.serialisertTilString(),
                ContentType.Application.Json,
                HttpStatusCode.OK,
            )
        }

        /** Slett sykepengegrunnlag */
        delete {
            service.slettSykepengegrunnlag(call.periodeReferanse(), call.saksbehandler())
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
