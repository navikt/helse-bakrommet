package no.nav.helse.bakrommet.inntektsmelding

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import no.nav.helse.bakrommet.Configuration
import no.nav.helse.bakrommet.auth.OboToken
import no.nav.helse.bakrommet.errorhandling.ForbiddenException
import no.nav.helse.bakrommet.util.logg
import no.nav.helse.bakrommet.util.sikkerLogger
import java.time.LocalDate
import java.util.*

class InntektsmeldingClient(
    private val configuration: Configuration.Inntektsmelding,
    private val httpClient: HttpClient =
        HttpClient(Apache) {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter())
            }
        },
) {
    suspend fun hentInntektsmeldinger(
        fnr: String,
        fom: LocalDate,
        tom: LocalDate,
        inntektsmeldingToken: OboToken,
    ): JsonNode {
        val callId: String = UUID.randomUUID().toString()
        val callIdDesc = " callId=$callId"
        val response =
            httpClient.post("${configuration.baseUrl}/api/v1/inntektsmelding/soek") {
                headers[HttpHeaders.Authorization] = inntektsmeldingToken.somBearerHeader()
                header("Nav-Consumer-Id", "bakrommet-speilvendt")
                header("no.nav.consumer.id", "bakrommet-speilvendt")
                header("Nav-Call-Id", callId)
                header("no.nav.callid", callId)
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("fnr", fnr)
                        put("fom", fom.toString())
                        put("tom", tom.toString())
                    }.toString(),
                )
            }
        if (response.status == HttpStatusCode.OK) {
            logg.info("Got response from inntektsmelding-API $callIdDesc")
            return response.body<JsonNode>()
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
