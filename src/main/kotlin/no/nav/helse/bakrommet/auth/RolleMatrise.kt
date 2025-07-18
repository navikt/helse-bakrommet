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
            path = "/v1/{personId}/saksbehandlingsperioder/(method:POST)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{personId}/saksbehandlingsperioder/(method:GET)",
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
        // Inntektsmeldinger
        Rule(
            path = "/v1/{personId}/inntektsmeldinger/(method:GET)",
            role = setOf(SAKSBEHANDLER),
        ),
        // Saksbehandlingsperioder - ikke knyttet til spesifikk person
        Rule(
            path = "/v1/saksbehandlingsperioder/(method:GET)",
            role = setOf(LES, SAKSBEHANDLER, BESLUTTER),
        ),
        // Saksbehandlingsperioder - spesifikke endepunkter
        Rule(
            path = "/v1/{personId}/saksbehandlingsperioder/{periodeUUID}/(method:GET)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{personId}/saksbehandlingsperioder/{periodeUUID}/historikk/(method:GET)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{personId}/saksbehandlingsperioder/{periodeUUID}/dokumenter/(method:GET)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{personId}/saksbehandlingsperioder/{periodeUUID}/dokumenter/{dokumentUUID}/(method:GET)",
            role = setOf(SAKSBEHANDLER),
        ),
        // Vilkår
        Rule(
            path = "/v1/{personId}/saksbehandlingsperioder/{periodeUUID}/vilkaar/(method:GET)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{personId}/saksbehandlingsperioder/{periodeUUID}/vilkaar/{kode}/(method:PUT)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{personId}/saksbehandlingsperioder/{periodeUUID}/vilkaar/{kode}/(method:DELETE)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{personId}/saksbehandlingsperioder/{periodeUUID}/sendtilbeslutning/(method:POST)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{personId}/saksbehandlingsperioder/{periodeUUID}/tatilbeslutning/(method:POST)",
            role = setOf(BESLUTTER),
        ),
        Rule(
            path = "/v1/{personId}/saksbehandlingsperioder/{periodeUUID}/sendtilbake/(method:POST)",
            role = setOf(BESLUTTER),
        ),
        Rule(
            path = "/v1/{personId}/saksbehandlingsperioder/{periodeUUID}/godkjenn/(method:POST)",
            role = setOf(BESLUTTER),
        ),
        // Inntektsforhold
        Rule(
            path = "/v1/{personId}/saksbehandlingsperioder/{periodeUUID}/inntektsforhold/(method:GET)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{personId}/saksbehandlingsperioder/{periodeUUID}/inntektsforhold/(method:POST)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{personId}/saksbehandlingsperioder/{periodeUUID}/inntektsforhold/{inntektsforholdUUID}/dagoversikt/(method:PUT)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{personId}/saksbehandlingsperioder/{periodeUUID}/inntektsforhold/{inntektsforholdUUID}/kategorisering/(method:PUT)",
            role = setOf(SAKSBEHANDLER),
        ),
        // Dokumenter - relaterte ruter
        Rule(
            path = "/v1/{personId}/saksbehandlingsperioder/{periodeUUID}/dokumenter/ainntekt/hent/(method:POST)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{personId}/saksbehandlingsperioder/{periodeUUID}/dokumenter/arbeidsforhold/hent/(method:POST)",
            role = setOf(SAKSBEHANDLER),
        ),
        Rule(
            path = "/v1/{personId}/saksbehandlingsperioder/{periodeUUID}/dokumenter/pensjonsgivendeinntekt/hent/(method:POST)",
            role = setOf(SAKSBEHANDLER),
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
