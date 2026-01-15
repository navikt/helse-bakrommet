package no.nav.helse.bakrommet.obo

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import no.nav.helse.bakrommet.client.common.mockHttpClient

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

private suspend fun HttpRequestData.bodyToJson(): JsonNode = jacksonObjectMapper().readValue(body.toByteArray(), JsonNode::class.java)
