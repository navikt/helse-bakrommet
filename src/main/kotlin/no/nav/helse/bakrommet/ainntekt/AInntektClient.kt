package no.nav.helse.bakrommet.ainntekt

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
import java.time.YearMonth
import java.util.*

class AInntektClient(
    private val configuration: Configuration.AInntekt,
    private val httpClient: HttpClient =
        HttpClient(Apache) {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter())
            }
            engine {
                connectionRequestTimeout = 20_000
                socketTimeout = 20_000 // default 10_000
            }
        },
) {
    suspend fun hentInntekterFor(
        fnr: String,
        maanedFom: YearMonth,
        maanedTom: YearMonth,
        ainntektToken: OboToken,
    ): JsonNode {
        val callId: String = UUID.randomUUID().toString()
        val callIdDesc = " callId=$callId"
        val response =
            httpClient.post("https://${configuration.hostname}/rs/api/v1/hentinntektliste") {
                headers[HttpHeaders.Authorization] = ainntektToken.somBearerHeader()
                header("Nav-Consumer-Id", "bakrommet-speilvendt")
                header("Nav-Call-Id", callId)
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put(
                            "ident",
                            buildJsonObject {
                                put("identifikator", fnr)
                                put("aktoerType", "NATURLIG_IDENT")
                            },
                        )
                        put("ainntektsfilter", "8-28") // TODO: Ev. 8-30 eller annet ?
                        put("formaal", "Sykepenger")
                        put("maanedFom", maanedFom.toString())
                        put("maanedTom", maanedTom.toString())
                    }.toString(),
                )
            }
        if (response.status == HttpStatusCode.OK) {
            logg.info("Got response from inntektskomponenten $callIdDesc")
            return response.body<JsonNode>()
        } else {
            logg.error("Feil under henting av inntekter: ${response.status}, Se secureLog for detaljer $callIdDesc")
            sikkerLogger.error("Feil under henting av inntekter: ${response.status} - ${response.bodyAsText()} $callIdDesc")
            if (response.status == HttpStatusCode.Forbidden) {
                throw ForbiddenException("Ikke tilstrekkelig tilgang i A-Inntekt")
            }
            throw RuntimeException(
                "Feil ved henting av arbeidsforhold, status=${response.status.value}, callId=$callId",
            )
        }
    }
}
