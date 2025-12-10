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
            path = "/v1/{personId}/behandlinger/(method:POST)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{personId}/behandlinger/(method:GET)",
            role = setOf(SAKSBEHANDLER),
        ),
        // Personsøk og personinfo
        Rule(
            path = "/v1/personsok/(method:POST)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{personId}/personinfo/(method:GET)",
            role = setOf(SAKSBEHANDLER),
        ),
        // Søknader
        Rule(
            path = "/v1/{personId}/soknader/(method:GET)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{personId}/soknader/{soknadId}/(method:GET)",
            role = setOf(SAKSBEHANDLER),
        ),
        // Saksbehandlingsperioder - ikke knyttet til spesifikk person
        Rule(
            path = "/v1/behandlinger/(method:GET)",
            role = setOf(LES, SAKSBEHANDLER, BESLUTTER),
        ),
        // Saksbehandlingsperioder - spesifikke endepunkter
        Rule(
            path = "/v1/{personId}/behandlinger/{periodeUUID}/(method:GET)",
            role = setOf(SAKSBEHANDLER, LES, BESLUTTER),
        ),
        Rule(
            path = "/v1/{personId}/behandlinger/{periodeUUID}/historikk/(method:GET)",
            role = setOf(SAKSBEHANDLER, LES, BESLUTTER),
        ),
        // Sykepengegrunnlag
        Rule(
            path = "/v2/{personId}/behandlinger/{periodeUUID}/sykepengegrunnlag/(method:GET)",
            role = setOf(SAKSBEHANDLER, LES, BESLUTTER),
        ),
        Rule(
            path = "/v2/{personId}/behandlinger/{periodeUUID}/sykepengegrunnlag/(method:DELETE)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v2/{personId}/behandlinger/{periodeUUID}/sykepengegrunnlag/(method:POST)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{personId}/behandlinger/{periodeUUID}/dokumenter/(method:GET)",
            role = setOf(SAKSBEHANDLER, LES, BESLUTTER),
        ),
        Rule(
            path = "/v1/{personId}/behandlinger/{periodeUUID}/utbetalingsberegning/(method:GET)",
            role = setOf(SAKSBEHANDLER, LES, BESLUTTER),
        ),
        Rule(
            path = "/v1/{personId}/behandlinger/{periodeUUID}/dokumenter/{dokumentUUID}/(method:GET)",
            role = setOf(SAKSBEHANDLER, LES, BESLUTTER),
        ),
        // Vilkårsvurdering
        Rule(
            path = "/v1/{personId}/behandlinger/{periodeUUID}/vilkaarsvurdering/(method:GET)",
            role = setOf(SAKSBEHANDLER, LES, BESLUTTER),
        ),
        Rule(
            path = "/v1/{personId}/behandlinger/{periodeUUID}/vilkaarsvurdering/{hovedspørsmål}/(method:PUT)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{personId}/behandlinger/{periodeUUID}/vilkaarsvurdering/{hovedspørsmål}/(method:DELETE)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{personId}/behandlinger/{periodeUUID}/sendtilbeslutning/(method:POST)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{personId}/behandlinger/{periodeUUID}/revurder/(method:POST)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{personId}/behandlinger/{periodeUUID}/tatilbeslutning/(method:POST)",
            role = setOf(BESLUTTER),
        ),
        Rule(
            path = "/v1/{personId}/behandlinger/{periodeUUID}/sendtilbake/(method:POST)",
            role = setOf(BESLUTTER),
        ),
        Rule(
            path = "/v1/{personId}/behandlinger/{periodeUUID}/godkjenn/(method:POST)",
            role = setOf(BESLUTTER),
        ),
        Rule(
            path = "/v1/{personId}/behandlinger/{periodeUUID}/skjaeringstidspunkt/(method:PUT)",
            role = setOf(SAKSBEHANDLER),
        ),
        // Yrkesaktivitet
        Rule(
            path = "/v1/{personId}/behandlinger/{periodeUUID}/yrkesaktivitet/(method:GET)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{personId}/behandlinger/{periodeUUID}/yrkesaktivitet/(method:POST)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{personId}/behandlinger/{periodeUUID}/yrkesaktivitet/{yrkesaktivitetUUID}/dagoversikt/(method:PUT)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{personId}/behandlinger/{periodeUUID}/yrkesaktivitet/{yrkesaktivitetUUID}/refusjon/(method:PUT)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{personId}/behandlinger/{periodeUUID}/yrkesaktivitet/{yrkesaktivitetUUID}/kategorisering/(method:PUT)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{personId}/behandlinger/{periodeUUID}/yrkesaktivitet/{yrkesaktivitetUUID}/perioder/(method:PUT)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{personId}/behandlinger/{periodeUUID}/yrkesaktivitet/{yrkesaktivitetUUID}/inntekt/(method:PUT)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{personId}/behandlinger/{periodeUUID}/yrkesaktivitet/{yrkesaktivitetUUID}/(method:DELETE)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{personId}/behandlinger/{periodeUUID}/yrkesaktivitet/{yrkesaktivitetUUID}/inntektsmeldinger/(method:GET)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{personId}/behandlinger/{periodeUUID}/yrkesaktivitet/{yrkesaktivitetUUID}/pensjonsgivendeinntekt/(method:GET)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{personId}/behandlinger/{periodeUUID}/yrkesaktivitet/{yrkesaktivitetUUID}/ainntekt/(method:GET)",
            role = setOf(SAKSBEHANDLER),
        ),
        // Dokumenter - relaterte ruter
        Rule(
            path = "/v1/{personId}/behandlinger/{periodeUUID}/dokumenter/ainntekt/hent-8-28/(method:POST)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{personId}/behandlinger/{periodeUUID}/dokumenter/ainntekt/hent-8-30/(method:POST)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{personId}/behandlinger/{periodeUUID}/dokumenter/arbeidsforhold/hent/(method:POST)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{personId}/behandlinger/{periodeUUID}/dokumenter/pensjonsgivendeinntekt/hent/(method:POST)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{personId}/behandlinger/{periodeUUID}/tilkommeninntekt/(method:GET)",
            role = setOf(SAKSBEHANDLER, BESLUTTER, LES),
        ),
        Rule(
            path = "/v1/{personId}/behandlinger/{periodeUUID}/tilkommeninntekt/(method:POST)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{personId}/behandlinger/{periodeUUID}/tilkommeninntekt/{tilkommenInntektId}/(method:PUT)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{personId}/behandlinger/{periodeUUID}/tilkommeninntekt/{tilkommenInntektId}/(method:DELETE)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v2/{personId}/tidslinje/(method:GET)",
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
