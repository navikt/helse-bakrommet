package no.nav.helse.bakrommet.obo

import io.ktor.client.engine.mock.*
import io.ktor.http.*
import no.nav.helse.bakrommet.Configuration
import no.nav.helse.bakrommet.auth.OboClient
import no.nav.helse.bakrommet.bodyToJson
import no.nav.helse.bakrommet.mockHttpClient

class OboTestSetup(
    val userToken: String,
    val oboConfiguration: Configuration.OBO,
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
                oboConfiguration = Configuration.OBO(url = "OBO-url"),
            )
    }
}
