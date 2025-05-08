package no.nav.helse.bakrommet

import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.helse.bakrommet.auth.OAuthMock
import no.nav.helse.bakrommet.auth.OboClient
import no.nav.helse.bakrommet.db.TestcontainersDatabase
import no.nav.helse.bakrommet.pdl.PdlClient
import no.nav.helse.bakrommet.pdl.PdlMock
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadBackendClient

object TestOppsett {
    val oAuthMock = OAuthMock()

    val configuration =
        Configuration(
            TestcontainersDatabase.configuration,
            Configuration.OBO(url = "OBO-url"),
            Configuration.PDL(hostname = "PDL-hostname", scope = "PDL-scope"),
            oAuthMock.authConfig,
            Configuration.SykepengesoknadBackend(
                hostname = "sykepengesoknad-backend",
                scope = "sykepengesoknad-backend-scope",
            ),
            "test",
        )
    val oboToken = "OBO-TOKEN"
    val userToken = oAuthMock.token()

    val mockTexas =
        mockHttpClient { request ->
            if (request.bodyToJson()["user_token"].asText() != userToken) {
                respondError(HttpStatusCode.Unauthorized)
            } else {
                respond(
                    status = HttpStatusCode.OK,
                    content =
                        """
                        {"access_token": "$oboToken"}
                        """.trimIndent(),
                    headers = headersOf("Content-Type" to listOf("application/json")),
                )
            }
        }

    val oboClient = OboClient(configuration.obo, mockTexas)
}

fun runApplicationTest(
    config: Configuration = TestOppsett.configuration,
    pdlClient: PdlClient = PdlMock.pdlClient,
    oboClient: OboClient = TestOppsett.oboClient,
    sykepengesoknadBackendClient: SykepengesoknadBackendClient =
        SykepengesoknadBackendClient(
            configuration = Configuration.SykepengesoknadBackend("soknadHost", "soknadScope"),
        ),
    testBlock: suspend ApplicationTestBuilder.() -> Unit,
) = testApplication {
    personsokModule(config, pdlClient, oboClient, sykepengesoknadBackendClient)
    testBlock()
}

fun ApplicationTestBuilder.personsokModule(
    config: Configuration,
    pdlClient: PdlClient,
    oboClient: OboClient,
    sykepengesoknadBackendClient: SykepengesoknadBackendClient,
) {
    application {
        settOppKtor(
            instansierDatabase(config.db),
            config,
            pdlClient = pdlClient,
            oboClient = oboClient,
            sykepengesoknadBackendClient = sykepengesoknadBackendClient,
        )
    }
}
