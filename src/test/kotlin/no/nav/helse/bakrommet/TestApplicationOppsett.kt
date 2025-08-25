package no.nav.helse.bakrommet

import com.auth0.jwt.JWT
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.testing.*
import no.nav.helse.bakrommet.Configuration.Roller
import no.nav.helse.bakrommet.aareg.AARegClient
import no.nav.helse.bakrommet.aareg.AARegMock
import no.nav.helse.bakrommet.ainntekt.AInntektClient
import no.nav.helse.bakrommet.ainntekt.AInntektMock
import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.auth.OAuthMock
import no.nav.helse.bakrommet.auth.OAuthScope
import no.nav.helse.bakrommet.auth.OboClient
import no.nav.helse.bakrommet.auth.SpilleromBearerToken
import no.nav.helse.bakrommet.auth.tilRoller
import no.nav.helse.bakrommet.db.TestDataSource
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingApiMock
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingClient
import no.nav.helse.bakrommet.pdl.PdlClient
import no.nav.helse.bakrommet.pdl.PdlMock
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.DokumentDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetDao
import no.nav.helse.bakrommet.sigrun.SigrunClient
import no.nav.helse.bakrommet.sigrun.SigrunMock
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadBackendClient
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadMock
import no.nav.helse.bakrommet.util.objectMapper
import javax.sql.DataSource

object TestOppsett {
    val oAuthMock = OAuthMock()

    val configuration =
        Configuration(
            db = Configuration.DB(jdbcUrl = TestDataSource.dbModule.jdbcUrl),
            obo = Configuration.OBO(url = "OBO-url"),
            pdl = Configuration.PDL(hostname = "PDL-hostname", scope = OAuthScope("PDL-scope")),
            auth = oAuthMock.authConfig,
            sykepengesoknadBackend =
                Configuration.SykepengesoknadBackend(
                    hostname = "sykepengesoknad-backend",
                    scope = OAuthScope("sykepengesoknad-backend-scope"),
                ),
            aareg =
                Configuration.AAReg(
                    hostname = "aareg-host",
                    scope = OAuthScope("aareg-scope"),
                ),
            ainntekt =
                Configuration.AInntekt(
                    hostname = "inntektskomponenten-host",
                    scope = OAuthScope("inntektskomponenten-scope"),
                ),
            inntektsmelding =
                Configuration.Inntektsmelding(
                    baseUrl = "http://localhost",
                    scope = OAuthScope("im-scope"),
                ),
            sigrun =
                Configuration.Sigrun(
                    baseUrl = "http://localhost",
                    scope = OAuthScope("sigrun-scope"),
                ),
            roller =
                Roller(
                    les = setOf("GRUPPE_LES"),
                    saksbehandler = setOf("GRUPPE_SAKSBEHANDLER"),
                    beslutter = setOf("GRUPPE_BESLUTTER"),
                ),
            naisClusterName = "test",
        )
    val userToken = oAuthMock.token()

    fun brukerFraToken(token: String) =
        token.let { token ->
            val payload = JWT.decode(token)
            Bruker(
                navn = payload.getClaim("name").asString(),
                navIdent = payload.getClaim("NAVident").asString(),
                preferredUsername = payload.getClaim("preferred_username").asString(),
                roller = payload.getClaim("groups").asList(String::class.java).toSet().tilRoller(configuration.roller),
            )
        }

    val userTokenOgBruker =
        BrukerOgToken(
            token = SpilleromBearerToken(userToken),
            bruker = brukerFraToken(userToken),
        )

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
    val yrkesaktivitetDao: YrkesaktivitetDao,
) {
    companion object {
        fun instansier(dataSource: DataSource): Daoer {
            return Daoer(
                PersonDao(dataSource),
                SaksbehandlingsperiodeDao(dataSource),
                DokumentDao(dataSource),
                YrkesaktivitetDao(dataSource),
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
    sigrunClient: SigrunClient = SigrunMock.sigrunMockClient(),
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
            sigrunClient = sigrunClient,
        )
    }
    client =
        createClient {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(objectMapper))
            }
        }

    testBlock(Daoer.instansier(dataSource))
}
