package no.nav.helse.bakrommet.obo

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.JacksonConverter
import no.nav.helse.bakrommet.util.objectMapper

class OboTestSetup(
    val userToken: String,
    val oboConfiguration: OboModule.Configuration,
) {
    fun oboTokenFraMock(scope: String): String = "OBO-TOKEN_FOR_$scope"

    val mockTexas =
        mockHttpClient { request ->
            if (request.bodyToJson()["user_token"].asText() != userToken) {
                respondError(HttpStatusCode.Unauthorized)
            } else {
                val scope = request.bodyToJson()["target"].asText()
                respond(
                    status = HttpStatusCode.OK,
                    content = """{"access_token": "${oboTokenFraMock(scope)}"}""".trimIndent(),
                    headers = headersOf("Content-Type" to listOf("application/json")),
                )
            }
        }

    val oboClient = OboClient(oboConfiguration, mockTexas)

    companion object {
        fun create(userToken: String): OboTestSetup =
            OboTestSetup(
                userToken = userToken,
                oboConfiguration = OboModule.Configuration(url = "OBO-url"),
            )
    }
}

private fun mockHttpClient(requestHandler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
    HttpClient(MockEngine) {
        install(ContentNegotiation) {
            register(ContentType.Application.Json, JacksonConverter(objectMapper))
        }
        engine {
            addHandler(requestHandler)
        }
    }

private suspend fun HttpRequestData.bodyToJson(): JsonNode = jacksonObjectMapper().readValue(body.toByteArray(), JsonNode::class.java)
