package no.nav.helse.bakrommet

import com.zaxxer.hikari.HikariConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import no.nav.helse.bakrommet.aareg.AARegClient
import no.nav.helse.bakrommet.aareg.AARegMock
import no.nav.helse.bakrommet.ainntekt.AInntektClient
import no.nav.helse.bakrommet.ainntekt.AInntektMock
import no.nav.helse.bakrommet.api.ApiModule
import no.nav.helse.bakrommet.api.settOppKtor
import no.nav.helse.bakrommet.auth.OAuthMock
import no.nav.helse.bakrommet.auth.OAuthScope
import no.nav.helse.bakrommet.behandling.dokumenter.DokumentDao
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagDao
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetDao
import no.nav.helse.bakrommet.client.common.ApplicationConfig
import no.nav.helse.bakrommet.db.DBModule
import no.nav.helse.bakrommet.db.TestDataSource
import no.nav.helse.bakrommet.db.dao.*
import no.nav.helse.bakrommet.db.skapDbDaoer
import no.nav.helse.bakrommet.ereg.EregMock
import no.nav.helse.bakrommet.infrastruktur.provider.*
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingApiMock
import no.nav.helse.bakrommet.kafka.OutboxDao
import no.nav.helse.bakrommet.obo.OboModule
import no.nav.helse.bakrommet.obo.OboTestSetup
import no.nav.helse.bakrommet.pdl.PdlMock
import no.nav.helse.bakrommet.person.PersonPseudoIdDao
import no.nav.helse.bakrommet.sigrun.SigrunClientModule
import no.nav.helse.bakrommet.sigrun.SigrunMock
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadMock
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesøknadBackendClientModule
import javax.sql.DataSource
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object TestOppsett {
    val oAuthMock = OAuthMock()

    val configuration =
        Configuration(
            db = DBModule.Configuration(jdbcUrl = TestDataSource.dbModule.jdbcUrl),
            obo = OboModule.Configuration(url = "OBO-url"),
            pdl = PdlMock.defaultConfiguration,
            api =
                ApiModule.Configuration(
                    auth = oAuthMock.authConfig,
                    roller =
                        ApiModule.Configuration.Roller(
                            les = setOf("GRUPPE_LES"),
                            saksbehandler = setOf("GRUPPE_SAKSBEHANDLER"),
                            beslutter = setOf("GRUPPE_BESLUTTER"),
                        ),
                ),
            sykepengesoknadBackend =
                SykepengesøknadBackendClientModule.Configuration(
                    hostname = "sykepengesoknad-backend",
                    scope = OAuthScope("sykepengesoknad-backend-scope"),
                    appConfig =
                        ApplicationConfig(
                            podName = "unknownHost",
                            appName = "unknownApp",
                            imageName = "unknownImage",
                        ),
                ),
            aareg = AARegMock.defaultConfiguration,
            ainntekt = AInntektMock.defaultConfiguration,
            ereg = EregMock.defaultConfiguration,
            inntektsmelding = InntektsmeldingApiMock.defaultConfiguration,
            sigrun =
                SigrunClientModule.Configuration(
                    baseUrl = "http://localhost",
                    scope = OAuthScope("sigrun-scope"),
                    appConfig =
                        ApplicationConfig(
                            podName = "unknownHost",
                            appName = "unknownApp",
                            imageName = "unknownImage",
                        ),
                ),
            naisClusterName = "test",
        )
    val userToken = oAuthMock.token()

    fun OAuthScope.oboTokenFor() = "OBO-TOKEN_FOR_api://$baseValue/.default"

    val oboSetup = OboTestSetup.create(userToken)
    val oboClient = oboSetup.oboClient
}

class Daoer(
    val personPseudoIdDao: PersonPseudoIdDao,
    val dokumentDao: DokumentDao,
    val yrkesaktivitetDao: YrkesaktivitetDao,
    val outboxDao: OutboxDao,
    val sykepengegrunnlagDao: SykepengegrunnlagDao,
) {
    companion object {
        fun instansier(dataSource: DataSource): Daoer =
            Daoer(
                PersonPseudoIdDaoPg(dataSource),
                DokumentDaoPg(dataSource),
                YrkesaktivitetDaoPg(dataSource),
                OutboxDaoPg(dataSource),
                SykepengegrunnlagDaoPg(dataSource),
            )
    }
}

fun runApplicationTest(
    config: Configuration = TestOppsett.configuration,
    dataSource: DataSource = instansierDatabase(config.db),
    pdlClient: PersoninfoProvider = PdlMock.pdlClient(),
    resetDatabase: Boolean = true,
    sykepengesøknadProvider: SykepengesøknadProvider = SykepengesoknadMock.sykepengersoknadBackendClientMock(tokenUtvekslingProvider = TestOppsett.oboClient),
    aaRegClient: AARegClient = AARegMock.aaRegClientMock(),
    aInntektClient: AInntektClient =
        AInntektMock.aInntektClientMock(fnrTilAInntektResponse = emptyMap()),
    organisasjonsnavnProvider: OrganisasjonsnavnProvider = EregMock.eregClientMock(),
    pensjonsgivendeInntektProvider: PensjonsgivendeInntektProvider = SigrunMock.sigrunMockClient(),
    inntektsmeldingClient: InntektsmeldingProvider = InntektsmeldingApiMock.inntektsmeldingClientMock(),
    testBlock: suspend ApplicationTestBuilder.(daoer: Daoer) -> Unit,
) = testApplication {
    if (resetDatabase) {
        TestDataSource.resetDatasource()
    }
    application {
        val providers =
            Providers(
                personinfoProvider = pdlClient,
                sykepengesøknadProvider = sykepengesøknadProvider,
                inntekterProvider = aInntektClient,
                arbeidsforholdProvider = aaRegClient,
                organisasjonsnavnProvider = organisasjonsnavnProvider,
                inntektsmeldingProvider = inntektsmeldingClient,
                pensjonsgivendeInntektProvider = pensjonsgivendeInntektProvider,
            )
        val db = skapDbDaoer(dataSource)
        val services =
            createServices(
                providers = providers,
                db = db,
            )

        settOppKtor(config.api, services, db, errorHandlingIncludeStackTrace = true)
    }
    client =
        createClient {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(objectMapper))
            }
        }

    testBlock(Daoer.instansier(dataSource))
}

// Manglende funksjoner fra App.kt
internal fun instansierDatabase(configuration: DBModule.Configuration) = DBModule(configuration = configuration, ::testHikariConfigurator).also { it.migrate() }.dataSource

private fun testHikariConfigurator(configuration: DBModule.Configuration) =
    HikariConfig().apply {
        jdbcUrl = configuration.jdbcUrl
        maximumPoolSize = 2
        minimumIdle = 1
        idleTimeout = 10.seconds.inWholeMilliseconds
        maxLifetime = idleTimeout * 5
        initializationFailTimeout = 1.minutes.inWholeMilliseconds
        connectionTimeout = 5.seconds.inWholeMilliseconds
        leakDetectionThreshold = 30.seconds.inWholeMilliseconds
    }
