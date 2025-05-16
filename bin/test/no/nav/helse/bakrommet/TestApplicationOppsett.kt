package no.nav.helse.bakrommet

import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.helse.bakrommet.aareg.AARegClient
import no.nav.helse.bakrommet.aareg.AARegMock
import no.nav.helse.bakrommet.ainntekt.AInntektClient
import no.nav.helse.bakrommet.ainntekt.AInntektMock
import no.nav.helse.bakrommet.auth.OAuthMock
import no.nav.helse.bakrommet.auth.OboClient
import no.nav.helse.bakrommet.db.TestDataSource
import no.nav.helse.bakrommet.pdl.PdlClient
import no.nav.helse.bakrommet.pdl.PdlMock
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadBackendClient
import javax.sql.DataSource

object TestOppsett {
    val oAuthMock = OAuthMock()

    val configuration =
        Configuration(
            Configuration.DB(jdbcUrl = TestDataSource.dbModule.jdbcUrl),
            Configuration.OBO(url = "OBO-url"),
            Configuration.PDL(hostname = "PDL-hostname", scope = "PDL-scope"),
            oAuthMock.authConfig,
            Configuration.SykepengesoknadBackend(
                hostname = "sykepengesoknad-backend",
                scope = "sykepengesoknad-backend-scope",
            ),
            Configuration.AAReg(
                hostname = "aareg-host",
                scope = "aareg-scope",
            ),
            Configuration.AInntekt(
                hostname = "inntektskomponenten-host",
                scope = "inntektskomponenten-scope",
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

class Daoer(
    val personDao: PersonDao,
    val saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao,
) {
    companion object {
        fun instansier(dataSource: DataSource): Daoer {
            return Daoer(
                PersonDao(dataSource),
                SaksbehandlingsperiodeDao(dataSource),
            )
        }
    }
}

fun runApplicationTest(
    config: Configuration = TestOppsett.configuration,
    dataSource: DataSource = instansierDatabase(config.db),
    pdlClient: PdlClient = PdlMock.pdlClient,
    oboClient: OboClient = TestOppsett.oboClient,
    resetDatabase: Boolean = true,
    sykepengesoknadBackendClient: SykepengesoknadBackendClient =
        SykepengesoknadBackendClient(
            configuration = Configuration.SykepengesoknadBackend("soknadHost", "soknadScope"),
        ),
    aaRegClient: AARegClient = AARegMock.aaRegClientMock(),
    aInntektClient: AInntektClient = AInntektMock.aInntektClientMock(),
    testBlock: suspend ApplicationTestBuilder.(daoer: Daoer) -> Unit,
) = testApplication {
    if (resetDatabase) {
        TestDataSource.resetDatasource()
    }
    application {
        settOppKtor(
            dataSource,
            config,
            pdlClient = pdlClient,
            oboClient = oboClient,
            sykepengesoknadBackendClient = sykepengesoknadBackendClient,
            aaRegClient = aaRegClient,
            aInntektClient = aInntektClient,
        )
    }
    testBlock(Daoer.instansier(dataSource))
}
