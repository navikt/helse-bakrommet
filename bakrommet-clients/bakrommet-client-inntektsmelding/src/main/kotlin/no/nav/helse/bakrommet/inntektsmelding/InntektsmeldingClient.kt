package no.nav.helse.bakrommet.inntektsmelding

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson.JacksonConverter
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import no.nav.helse.bakrommet.auth.AccessToken
import no.nav.helse.bakrommet.auth.TokenUtvekslingProvider
import no.nav.helse.bakrommet.errorhandling.ForbiddenException
import no.nav.helse.bakrommet.infrastruktur.provider.InntektsmeldingProvider
import no.nav.helse.bakrommet.util.Kildespor
import no.nav.helse.bakrommet.util.logg
import no.nav.helse.bakrommet.util.sikkerLogger
import java.time.LocalDate
import java.util.*

class InntektsmeldingClient(
    private val configuration: InntektsmeldingClientModule.Configuration,
    private val tokenUtvekslingProvider: TokenUtvekslingProvider,
    private val httpClient: HttpClient =
        HttpClient(Apache) {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter())
            }
        },
) : InntektsmeldingProvider {
    private suspend fun AccessToken.veksle() = "Bearer " + tokenUtvekslingProvider.exchangeToken(this, configuration.scope).value

    override suspend fun hentInntektsmeldinger(
        fnr: String,
        fom: LocalDate?,
        tom: LocalDate?,
        saksbehandlerToken: AccessToken,
    ): JsonNode {
        val callId: String = UUID.randomUUID().toString()
        val callIdDesc = " callId=$callId"
        val response =
            httpClient.post("${configuration.baseUrl}/api/v1/inntektsmelding/soek") {
                headers[HttpHeaders.Authorization] = saksbehandlerToken.veksle()
                header("Nav-Consumer-Id", "bakrommet-speilvendt")
                header("no.nav.consumer.id", "bakrommet-speilvendt")
                header("Nav-Call-Id", callId)
                header("no.nav.callid", callId)
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("fnr", fnr)
                        fom?.let { put("fom", fom.toString()) }
                        tom?.let { put("tom", tom.toString()) }
                    }.toString(),
                )
            }
        if (response.status == HttpStatusCode.OK) {
            logg.info("Got response from inntektsmelding-API $callIdDesc")
            // TODO: Benytt https://github.com/navikt/inntektsmelding-kontrakt ?
            return response.body<JsonNode>()
        } else if (response.status == HttpStatusCode.NotFound) {
            logg.info("Got 404-response from  inntektsmelding-API $callIdDesc. Returning empty list.")
            return jacksonObjectMapper().createArrayNode() // TODO: Eller faktisk 404 ? (404="Ingen inntektsmeldinger funnet") ref: https://spinosaurus.intern.dev.nav.no/swagger
        } else {
            // TODO: Håndter: 400="Ugyldig fødselsnummer", (404="Ingen inntektsmeldinger funnet") ref: https://spinosaurus.intern.dev.nav.no/swagger

            logg.error("Feil under henting av inntektsmelding: ${response.status}, Se secureLog for detaljer $callIdDesc")
            sikkerLogger.error("Feil under henting av inntektsmelding: ${response.status} - ${response.bodyAsText()} $callIdDesc")
            if (response.status == HttpStatusCode.Forbidden) {
                throw ForbiddenException("Ikke tilstrekkelig tilgang til inntektsmelding")
            }
            throw RuntimeException(
                "Feil ved henting av inntektsmelding, status=${response.status.value}, callId=$callId",
            )
        }
    }

    override suspend fun hentInntektsmeldingMedSporing(
        inntektsmeldingId: String,
        saksbehandlerToken: AccessToken,
    ): Pair<JsonNode, Kildespor> {
        val callId: String = UUID.randomUUID().toString()
        val callIdDesc = " callId=$callId"
        val url = "${configuration.baseUrl}/api/v1/inntektsmelding/$inntektsmeldingId"
        val kildespor =
            Kildespor.fraHer(
                Throwable(),
                inntektsmeldingId,
                url,
                callId,
            ) // Inkluder saksbehandlerident?
        val response =
            httpClient.get(url) {
                headers[HttpHeaders.Authorization] = saksbehandlerToken.veksle()
                header("Nav-Consumer-Id", "bakrommet-speilvendt")
                header("no.nav.consumer.id", "bakrommet-speilvendt")
                header("Nav-Call-Id", callId)
                header("no.nav.callid", callId)
                accept(ContentType.Application.Json)
            }
        if (response.status == HttpStatusCode.OK) {
            logg.info("Got response from inntektsmelding-API $callIdDesc")
            // TODO: Benytt https://github.com/navikt/inntektsmelding-kontrakt ?
            return response.body<JsonNode>() to kildespor
        } else if (response.status == HttpStatusCode.NotFound) {
            throw RuntimeException("Inntektsmelding med id $inntektsmeldingId ikke funnet, callId=$callId")
        } else {
            logg.error("Feil under henting av inntektsmelding: ${response.status}, Se secureLog for detaljer $callIdDesc")
            sikkerLogger.error("Feil under henting av inntektsmelding: ${response.status} - ${response.bodyAsText()} $callIdDesc")
            if (response.status == HttpStatusCode.Forbidden) {
                throw ForbiddenException("Ikke tilstrekkelig tilgang til inntektsmelding")
            }
            throw RuntimeException(
                "Feil ved henting av inntektsmelding, status=${response.status.value}, callId=$callId",
            )
        }
    }
}
