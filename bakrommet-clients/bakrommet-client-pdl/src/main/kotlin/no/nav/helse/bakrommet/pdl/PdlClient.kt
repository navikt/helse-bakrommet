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
import no.nav.helse.bakrommet.auth.OboClient
import no.nav.helse.bakrommet.auth.SpilleromBearerToken
import no.nav.helse.bakrommet.errorhandling.PersonIkkeFunnetException
import no.nav.helse.bakrommet.infrastruktur.provider.PdlIdent
import no.nav.helse.bakrommet.infrastruktur.provider.PersonInfo
import no.nav.helse.bakrommet.infrastruktur.provider.PersoninfoProvider
import no.nav.helse.bakrommet.util.logg
import no.nav.helse.bakrommet.util.objectMapper
import no.nav.helse.bakrommet.util.sikkerLogger
import no.nav.helse.bakrommet.util.somListe

class PdlClient(
    private val configuration: Configuration.PDL,
    private val oboClient: OboClient,
    private val httpClient: HttpClient =
        HttpClient(Apache) {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter())
            }
        },
) : PersoninfoProvider {
    private suspend fun SpilleromBearerToken.tilOboBearerHeader(): String = this.exchangeWithObo(oboClient, configuration.scope).somBearerHeader()

    private val hentPersonQuery =
        """
        query(${"$"}ident: ID!){
          hentPerson(ident: ${"$"}ident) {
          	navn(historikk: false) {
          	  fornavn
          	  mellomnavn
          	  etternavn
            }
            foedselsdato {
                foedselsdato
            }
          }
        }
        """.trimIndent()

    private fun hentPersonRequest(ident: String): String {
        val m = jacksonObjectMapper()
        return m
            .createObjectNode()
            .apply {
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
        return m
            .createObjectNode()
            .apply {
                put("query", hentIdenterMedHistorikkQuery)
                set<ObjectNode>(
                    "variables",
                    m.createObjectNode().apply {
                        put("ident", ident)
                    },
                )
            }.toString()
    }

    override suspend fun hentIdenterFor(
        saksbehandlerToken: SpilleromBearerToken,
        ident: String,
    ): List<PdlIdent> {
        val response =
            httpClient.post("https://${configuration.hostname}/graphql") {
                headers[HttpHeaders.Authorization] = saksbehandlerToken.tilOboBearerHeader()
                contentType(ContentType.Application.Json)
                setBody(hentIdenterRequest(ident = ident))
            }
        if (response.status == HttpStatusCode.OK) {
            val json = response.body<JsonNode>()
            if (json.has("errors")) {
                val errors = json["errors"]
                // Finn første error med extensions.code == "not_found"
                val notFoundError =
                    errors.firstOrNull { errorNode ->
                        errorNode["extensions"]?.get("code")?.asText() == "not_found"
                    }

                if (notFoundError != null) {
                    // Kast spesifikk exception for "person ikke funnet"
                    val msg = notFoundError["message"].asText()
                    logg.warn("Person ikke funnet: {}", msg)
                    sikkerLogger.warn("hentIdenterFor not_found: {}", json)
                    throw PersonIkkeFunnetException()
                } else {
                    logg.warn("hentIdenterFor har andre errors")
                    sikkerLogger.warn("hentIdenterFor errors: {}", json)
                    throw RuntimeException("hentIdenterFor har errors: $json")
                }
            }

            return json["data"]["hentIdenter"]["identer"].somListe<PdlIdent>()
        }
        logg.warn("hentIdenterFor statusCode={}", response.status.value)
        throw RuntimeException("hentIdenterFor har statusCode ${response.status.value}")
    }

    override suspend fun hentPersonInfo(
        saksbehandlerToken: SpilleromBearerToken,
        ident: String,
    ): PersonInfo {
        val response =
            httpClient.post("https://${configuration.hostname}/graphql") {
                headers[HttpHeaders.Authorization] = saksbehandlerToken.tilOboBearerHeader()
                contentType(ContentType.Application.Json)
                setBody(hentPersonRequest(ident = ident))
                header("behandlingsnummer", "B139")
                header("TEMA", "SYK")
                header("Accept", "application/json")
            }
        if (response.status == HttpStatusCode.OK) {
            val json = response.body<String>()

            val parsedResponse = json.let { objectMapper.readValue<GraphQLResponse<HentPersonResponseData>>(it) }

            if ((parsedResponse.errors?.size ?: 0) > 0) {
                logg.warn("hentPersonInfo har errors")
                sikkerLogger.warn("hentPersonInfo har errors: {}", parsedResponse.hentErrors())
                throw RuntimeException("hentPersonInfo har errors")
            }
            if (parsedResponse.data.hentPerson?.navn == null) {
                logg.warn("hentPersonInfo har ingen data")
                throw RuntimeException("hentPersonInfo har ingen data")
            }
            if (parsedResponse.data.hentPerson.navn
                    .isEmpty()
            ) {
                logg.warn("hentPersonInfo har ingen navn")
                throw RuntimeException("hentPersonInfo har ingen navn")
            }
            return PersonInfo(
                navn =
                    parsedResponse.data.hentPerson.navn
                        .first(),
                fodselsdato =
                    parsedResponse.data.hentPerson.foedselsdato
                        ?.firstOrNull()
                        ?.foedselsdato,
            )
        } else {
            logg.warn("hentPersonInfo statusCode={}", response.status.value)
            throw RuntimeException("hentPersonInfo har statusCode ${response.status.value}")
        }
    }
}
