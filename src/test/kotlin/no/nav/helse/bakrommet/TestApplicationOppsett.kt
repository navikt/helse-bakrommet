package no.nav.helse.bakrommet

import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.helse.bakrommet.aareg.AARegClient
import no.nav.helse.bakrommet.aareg.AARegMock
import no.nav.helse.bakrommet.ainntekt.AInntektClient
import no.nav.helse.bakrommet.ainntekt.AInntektMock
import no.nav.helse.bakrommet.auth.OAuthMock
import no.nav.helse.bakrommet.auth.OAuthScope
import no.nav.helse.bakrommet.auth.OboClient
import no.nav.helse.bakrommet.db.TestDataSource
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingApiMock
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingClient
import no.nav.helse.bakrommet.pdl.PdlClient
import no.nav.helse.bakrommet.pdl.PdlMock
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.DokumentDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold.InntektsforholdDao
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadBackendClient
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadMock
import javax.sql.DataSource

object TestOppsett {
    val oAuthMock = OAuthMock()

    val configuration =
        Configuration(
            Configuration.DB(jdbcUrl = TestDataSource.dbModule.jdbcUrl),
            Configuration.OBO(url = "OBO-url"),
            Configuration.PDL(hostname = "PDL-hostname", scope = OAuthScope("PDL-scope")),
            oAuthMock.authConfig,
            Configuration.SykepengesoknadBackend(
                hostname = "sykepengesoknad-backend",
                scope = OAuthScope("sykepengesoknad-backend-scope"),
            ),
            Configuration.AAReg(
                hostname = "aareg-host",
                scope = OAuthScope("aareg-scope"),
            ),
            Configuration.AInntekt(
                hostname = "inntektskomponenten-host",
                scope = OAuthScope("inntektskomponenten-scope"),
            ),
            Configuration.Inntektsmelding(
                baseUrl = "http://localhost",
                scope = OAuthScope("im-scope"),
            ),
            "test",
        )
    val userToken = oAuthMock.token()

    fun OAuthScope.oboTokenFor() = "OBO-TOKEN_FOR_api://$baseValue/.default"

    private fun oboTokenFraMockTexas(scope: String) = "OBO-TOKEN_FOR_$scope"

    val mockTexas =
        mockHttpClient { request ->
            if (request.bodyToJson()["user_token"].asText() != userToken) {
                respondError(HttpStatusCode.Unauthorized)
            } else {
                val scope = request.bodyToJson()["target"].asText()
                respond(
                    status = HttpStatusCode.OK,
                    content =
                        """
                        {"access_token": "${oboTokenFraMockTexas(scope)}"}
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
    val dokumentDao: DokumentDao,
    val inntektsforholdDao: InntektsforholdDao,
) {
    companion object {
        fun instansier(dataSource: DataSource): Daoer {
            return Daoer(
                PersonDao(dataSource),
                SaksbehandlingsperiodeDao(dataSource),
                DokumentDao(dataSource),
                InntektsforholdDao(dataSource),
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
    sykepengesoknadBackendClient: SykepengesoknadBackendClient = SykepengesoknadMock.sykepengersoknadBackendClientMock(),
    aaRegClient: AARegClient = AARegMock.aaRegClientMock(),
    aInntektClient: AInntektClient = AInntektMock.aInntektClientMock(),
    inntektsmeldingClient: InntektsmeldingClient = InntektsmeldingApiMock.inntektsmeldingClientMock(),
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
            inntektsmeldingClient = inntektsmeldingClient,
        )
    }
    testBlock(Daoer.instansier(dataSource))
}
