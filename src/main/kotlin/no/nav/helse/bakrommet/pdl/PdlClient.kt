package no.nav.helse.bakrommet.pdl

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import no.nav.helse.bakrommet.Configuration
import no.nav.helse.bakrommet.appLogger
import no.nav.helse.bakrommet.sikkerLogger

class PdlClient(
    private val configuration: Configuration.PDL,
    private val httpClient: HttpClient = HttpClient(Apache) {
        install(ContentNegotiation) {
            register(ContentType.Application.Json, JacksonConverter())
        }
    }
) {
    private val hentIdenterMedHistorikkQuery =
        """
        query(${"$"}ident: ID!){
          hentIdenter(ident: ${"$"}ident, historikk: true) {
            identer {
              ident,
              gruppe
            }
          }
        }
        """.trimIndent()

    private fun hentIdenterRequest(ident: String): String {
        val m = jacksonObjectMapper()
        return m.createObjectNode().apply {
            put("query", hentIdenterMedHistorikkQuery)
            set<ObjectNode>(
                "variables",
                m.createObjectNode().apply {
                    put("ident", ident)
                },
            )
        }.toString()
    }

    suspend fun hentIdenterFor(
        pdlToken: String,
        ident: String,
    ): Set<String> {
        val response = httpClient.post("https://${configuration.hostname}/graphql") {
            headers[HttpHeaders.Authorization] = "Bearer $pdlToken"
            contentType(ContentType.Application.Json)
            setBody(hentIdenterRequest(ident = ident))
        }
        if (response.status == HttpStatusCode.OK) {
            val json = response.body<JsonNode>()
            if (json.has("errors")) {
                appLogger.warn("hentIdenterFor har errors")
                sikkerLogger.warn("hentIdenterFor har errors: {}", json)
                return emptySet()
            }
            return json["data"]["hentIdenter"]["identer"].map { it["ident"].asText() }.toSet()
        } else {
            appLogger.warn("hentIdenterFor statusCode={}", response.status.value)
        }
        return emptySet()
    }
}
