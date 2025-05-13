package no.nav.helse.bakrommet.aareg

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import no.nav.helse.bakrommet.Configuration
import no.nav.helse.bakrommet.auth.OboToken
import no.nav.helse.bakrommet.util.logg
import java.util.*

class AARegClient(
    private val configuration: Configuration.AAReg,
    private val httpClient: HttpClient =
        HttpClient(Apache) {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter())
            }
        },
) {
    suspend fun hentArbeidsforholdFor(
        fnr: String,
        aaregToken: OboToken,
    ): JsonNode {
        val callId: String = UUID.randomUUID().toString()
        val response =
            httpClient.get("https://${configuration.hostname}/api/v2/arbeidstaker/arbeidsforhold") {
                headers[HttpHeaders.Authorization] = aaregToken.somBearerHeader()
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header("Nav-Call-Id", callId)
                header("Nav-Personident", fnr)
                parameter("arbeidsforholdstatus", "AKTIV,AVSLUTTET,FREMTIDIG") // Default i V2 er: AKTIV,FREMTIDIG
                parameter("historikk", "true")
                parameter("regelverk", "ALLE")
            }
        if (response.status == HttpStatusCode.OK) {
            return response.body<JsonNode>()
        } else {
            logg.warn("hentArbeidsforholdFor statusCode={} callId={}", response.status.value, callId)
        }
        throw RuntimeException(
            "Feil ved henting av arbeidsforhold, status=${response.status.value}, callId=$callId",
        )
    }
}
