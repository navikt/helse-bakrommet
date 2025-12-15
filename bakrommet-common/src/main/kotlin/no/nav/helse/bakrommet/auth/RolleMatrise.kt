package no.nav.helse.bakrommet.auth

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.auth.Rolle.BESLUTTER
import no.nav.helse.bakrommet.auth.Rolle.LES
import no.nav.helse.bakrommet.auth.Rolle.SAKSBEHANDLER

data class Rule(
    val path: String,
    val role: Set<Rolle>,
)

val regler =
    listOf(
        Rule(
            path = "/v1/bruker/(method:GET)",
            role = setOf(LES, SAKSBEHANDLER, BESLUTTER),
        ),
        Rule(
            path = "/v1/organisasjon/{orgnummer}/(method:GET)",
            role = setOf(LES, SAKSBEHANDLER, BESLUTTER),
        ),
        Rule(
            path = "/v1/{pseudoId}/behandlinger/(method:POST)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{pseudoId}/behandlinger/(method:GET)",
            role = setOf(SAKSBEHANDLER),
        ),
        // Personsøk og personinfo
        Rule(
            path = "/v1/personsok/(method:POST)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{pseudoId}/personinfo/(method:GET)",
            role = setOf(SAKSBEHANDLER),
        ),
        // Søknader
        Rule(
            path = "/v1/{pseudoId}/soknader/(method:GET)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{pseudoId}/soknader/{soknadId}/(method:GET)",
            role = setOf(SAKSBEHANDLER),
        ),
        // Saksbehandlingsperioder - ikke knyttet til spesifikk person
        Rule(
            path = "/v1/behandlinger/(method:GET)",
            role = setOf(LES, SAKSBEHANDLER, BESLUTTER),
        ),
        // Saksbehandlingsperioder - spesifikke endepunkter
        Rule(
            path = "/v1/{pseudoId}/behandlinger/{behandlingId}/(method:GET)",
            role = setOf(SAKSBEHANDLER, LES, BESLUTTER),
        ),
        Rule(
            path = "/v1/{pseudoId}/behandlinger/{behandlingId}/historikk/(method:GET)",
            role = setOf(SAKSBEHANDLER, LES, BESLUTTER),
        ),
        // Sykepengegrunnlag
        Rule(
            path = "/v2/{pseudoId}/behandlinger/{behandlingId}/sykepengegrunnlag/(method:GET)",
            role = setOf(SAKSBEHANDLER, LES, BESLUTTER),
        ),
        Rule(
            path = "/v2/{pseudoId}/behandlinger/{behandlingId}/sykepengegrunnlag/(method:DELETE)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v2/{pseudoId}/behandlinger/{behandlingId}/sykepengegrunnlag/(method:POST)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{pseudoId}/behandlinger/{behandlingId}/dokumenter/(method:GET)",
            role = setOf(SAKSBEHANDLER, LES, BESLUTTER),
        ),
        Rule(
            path = "/v1/{pseudoId}/behandlinger/{behandlingId}/utbetalingsberegning/(method:GET)",
            role = setOf(SAKSBEHANDLER, LES, BESLUTTER),
        ),
        Rule(
            path = "/v1/{pseudoId}/behandlinger/{behandlingId}/dokumenter/{dokumentUUID}/(method:GET)",
            role = setOf(SAKSBEHANDLER, LES, BESLUTTER),
        ),
        // Vilkårsvurdering
        Rule(
            path = "/v1/{pseudoId}/behandlinger/{behandlingId}/vilkaarsvurdering/(method:GET)",
            role = setOf(SAKSBEHANDLER, LES, BESLUTTER),
        ),
        Rule(
            path = "/v1/{pseudoId}/behandlinger/{behandlingId}/vilkaarsvurdering/{hovedspørsmål}/(method:PUT)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{pseudoId}/behandlinger/{behandlingId}/vilkaarsvurdering/{hovedspørsmål}/(method:DELETE)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{pseudoId}/behandlinger/{behandlingId}/sendtilbeslutning/(method:POST)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{pseudoId}/behandlinger/{behandlingId}/revurder/(method:POST)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{pseudoId}/behandlinger/{behandlingId}/tatilbeslutning/(method:POST)",
            role = setOf(BESLUTTER),
        ),
        Rule(
            path = "/v1/{pseudoId}/behandlinger/{behandlingId}/sendtilbake/(method:POST)",
            role = setOf(BESLUTTER),
        ),
        Rule(
            path = "/v1/{pseudoId}/behandlinger/{behandlingId}/godkjenn/(method:POST)",
            role = setOf(BESLUTTER),
        ),
        Rule(
            path = "/v1/{pseudoId}/behandlinger/{behandlingId}/skjaeringstidspunkt/(method:PUT)",
            role = setOf(SAKSBEHANDLER),
        ),
        // Yrkesaktivitet
        Rule(
            path = "/v1/{pseudoId}/behandlinger/{behandlingId}/yrkesaktivitet/(method:GET)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{pseudoId}/behandlinger/{behandlingId}/yrkesaktivitet/(method:POST)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{pseudoId}/behandlinger/{behandlingId}/yrkesaktivitet/{yrkesaktivitetUUID}/dagoversikt/(method:PUT)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{pseudoId}/behandlinger/{behandlingId}/yrkesaktivitet/{yrkesaktivitetUUID}/refusjon/(method:PUT)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{pseudoId}/behandlinger/{behandlingId}/yrkesaktivitet/{yrkesaktivitetUUID}/kategorisering/(method:PUT)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{pseudoId}/behandlinger/{behandlingId}/yrkesaktivitet/{yrkesaktivitetUUID}/perioder/(method:PUT)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{pseudoId}/behandlinger/{behandlingId}/yrkesaktivitet/{yrkesaktivitetUUID}/inntekt/(method:PUT)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{pseudoId}/behandlinger/{behandlingId}/yrkesaktivitet/{yrkesaktivitetUUID}/(method:DELETE)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{pseudoId}/behandlinger/{behandlingId}/yrkesaktivitet/{yrkesaktivitetUUID}/inntektsmeldinger/(method:GET)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{pseudoId}/behandlinger/{behandlingId}/yrkesaktivitet/{yrkesaktivitetUUID}/pensjonsgivendeinntekt/(method:GET)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{pseudoId}/behandlinger/{behandlingId}/yrkesaktivitet/{yrkesaktivitetUUID}/ainntekt/(method:GET)",
            role = setOf(SAKSBEHANDLER),
        ),
        // Dokumenter - relaterte ruter
        Rule(
            path = "/v1/{pseudoId}/behandlinger/{behandlingId}/dokumenter/ainntekt/hent-8-28/(method:POST)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{pseudoId}/behandlinger/{behandlingId}/dokumenter/ainntekt/hent-8-30/(method:POST)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{pseudoId}/behandlinger/{behandlingId}/dokumenter/arbeidsforhold/hent/(method:POST)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{pseudoId}/behandlinger/{behandlingId}/dokumenter/pensjonsgivendeinntekt/hent/(method:POST)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{pseudoId}/behandlinger/{behandlingId}/tilkommeninntekt/(method:GET)",
            role = setOf(SAKSBEHANDLER, BESLUTTER, LES),
        ),
        Rule(
            path = "/v1/{pseudoId}/behandlinger/{behandlingId}/tilkommeninntekt/(method:POST)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{pseudoId}/behandlinger/{behandlingId}/tilkommeninntekt/{tilkommenInntektId}/(method:PUT)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{pseudoId}/behandlinger/{behandlingId}/tilkommeninntekt/{tilkommenInntektId}/(method:DELETE)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{pseudoId}/behandlinger/{behandlingId}/validering/(method:GET)",
            role = setOf(SAKSBEHANDLER, BESLUTTER, LES),
        ),
        Rule(
            path = "/v2/{pseudoId}/tidslinje/(method:GET)",
            role = setOf(SAKSBEHANDLER, BESLUTTER, LES),
        ),
    )

val RolleMatrise =
    createRouteScopedPlugin("RolleMatrise") {

        on(AuthenticationChecked) { call ->
            val r = (call as RoutingPipelineCall).route
            val routeAsString = r.toString()
            val rule =
                regler.firstOrNull { r ->
                    ("/(authenticate entraid)" + r.path) == routeAsString
                }

            if (rule == null) {
                println("Ingen regel funnet for $routeAsString")
                call.respond(HttpStatusCode.Forbidden, "Ingen regel funnet for denne forespørselen")
                return@on
            }

            val bruker = call.brukerPrincipal()
            if (bruker == null) {
                call.respond(HttpStatusCode.Unauthorized, "Bruker ikke autentisert")
                return@on
            }
            val roller = bruker.roller

            if (roller.intersect(rule.role).isEmpty()) {
                call.respond(HttpStatusCode.Forbidden, "Mangler riktig rolle for ${rule.path}")
                return@on
            }
        }
    }
