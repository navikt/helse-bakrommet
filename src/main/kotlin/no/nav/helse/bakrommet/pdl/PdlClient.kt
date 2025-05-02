package no.nav.helse.bakrommet.pdl

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
import no.nav.helse.bakrommet.util.objectMapper
import no.nav.helse.bakrommet.util.sikkerLogger

class PdlClient(
    private val configuration: Configuration.PDL,
    private val httpClient: HttpClient =
        HttpClient(Apache) {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter())
            }
        },
) {
    private val hentPersonQuery =
        """
        query(${"$"}ident: ID!){
          hentPerson(ident: ${"$"}ident) {
          	navn(historikk: false) {
          	  fornavn
          	  mellomnavn
          	  etternavn
            }
          }
        }
        """.trimIndent()

    private fun hentPersonRequest(ident: String): String {
        val m = jacksonObjectMapper()
        return m.createObjectNode().apply {
            put("query", hentPersonQuery)
            set<ObjectNode>(
                "variables",
                m.createObjectNode().apply {
                    put("ident", ident)
                },
            )
        }.toString()
    }

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
        pdlToken: OboToken,
        ident: String,
    ): Set<String> {
        val response =
            httpClient.post("https://${configuration.hostname}/graphql") {
                headers[HttpHeaders.Authorization] = pdlToken.somBearerHeader()
                contentType(ContentType.Application.Json)
                setBody(hentIdenterRequest(ident = ident))
            }
        if (response.status == HttpStatusCode.OK) {
            val json = response.body<JsonNode>()
            if (json.has("errors")) {
                logg.warn("hentIdenterFor har errors")
                sikkerLogger.warn("hentIdenterFor har errors: {}", json)
                return emptySet()
            }
            return json["data"]["hentIdenter"]["identer"].map { it["ident"].asText() }.toSet()
        } else {
            logg.warn("hentIdenterFor statusCode={}", response.status.value)
        }
        return emptySet()
    }

    data class PersonInfo(
        val navn: Navn,
    )

    suspend fun hentPersonInfo(
        pdlToken: OboToken,
        ident: String,
    ): PersonInfo {
        val response =
            httpClient.post("https://${configuration.hostname}/graphql") {
                headers[HttpHeaders.Authorization] = pdlToken.somBearerHeader()
                contentType(ContentType.Application.Json)
                setBody(hentPersonRequest(ident = ident))
                header("behandlingsnummer", "B139")
                header("TEMA", "SYK")
                header("Accept", "application/json")
            }
        if (response.status == HttpStatusCode.OK) {
            val json = response.body<String>()

            val parsedResponse = json.let { objectMapper.readValue<GraphQLResponse<HentNavnResponseData>>(it) }

            if ((parsedResponse.errors?.size ?: 0) > 0) {
                logg.warn("hentPersonInfo har errors")
                sikkerLogger.warn("hentPersonInfo har errors: {}", parsedResponse.hentErrors())
                throw RuntimeException("hentPersonInfo har errors")
            }
            if (parsedResponse.data.hentPerson?.navn == null) {
                logg.warn("hentPersonInfo har ingen data")
                throw RuntimeException("hentPersonInfo har ingen data")
            }
            if (parsedResponse.data.hentPerson.navn.isEmpty()) {
                logg.warn("hentPersonInfo har ingen navn")
                throw RuntimeException("hentPersonInfo har ingen navn")
            }
            return PersonInfo(
                navn = parsedResponse.data.hentPerson.navn.first(),
            )
        } else {
            logg.warn("hentPersonInfo statusCode={}", response.status.value)
            throw RuntimeException("hentPersonInfo har statusCode ${response.status.value}")
        }
    }
}
