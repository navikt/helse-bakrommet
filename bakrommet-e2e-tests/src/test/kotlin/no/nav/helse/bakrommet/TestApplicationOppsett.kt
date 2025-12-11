package no.nav.helse.bakrommet

import com.zaxxer.hikari.HikariConfig
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.testing.*
import no.nav.helse.bakrommet.Configuration.Roller
import no.nav.helse.bakrommet.aareg.AARegClient
import no.nav.helse.bakrommet.aareg.AARegMock
import no.nav.helse.bakrommet.ainntekt.AInntektClient
import no.nav.helse.bakrommet.ainntekt.AInntektMock
import no.nav.helse.bakrommet.api.setupApiRoutes
import no.nav.helse.bakrommet.auth.OAuthMock
import no.nav.helse.bakrommet.auth.OAuthScope
import no.nav.helse.bakrommet.behandling.dokumenter.DokumentDao
import no.nav.helse.bakrommet.behandling.dokumenter.DokumentDaoPg
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagDao
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagDaoPg
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetDao
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetDaoPg
import no.nav.helse.bakrommet.db.TestDataSource
import no.nav.helse.bakrommet.ereg.EregClient
import no.nav.helse.bakrommet.ereg.EregMock
import no.nav.helse.bakrommet.infrastruktur.db.DBModule
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingApiMock
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingClient
import no.nav.helse.bakrommet.kafka.OutboxDao
import no.nav.helse.bakrommet.kafka.OutboxDaoPg
import no.nav.helse.bakrommet.obo.OboTestSetup
import no.nav.helse.bakrommet.pdl.PdlClient
import no.nav.helse.bakrommet.pdl.PdlMock
import no.nav.helse.bakrommet.person.PersonPseudoIdDao
import no.nav.helse.bakrommet.person.PersonPseudoIdDaoPg
import no.nav.helse.bakrommet.sigrun.SigrunClient
import no.nav.helse.bakrommet.sigrun.SigrunMock
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadBackendClient
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadMock
import no.nav.helse.bakrommet.util.objectMapper
import javax.sql.DataSource
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object TestOppsett {
    val oAuthMock = OAuthMock()

    val configuration =
        Configuration(
            db = Configuration.DB(jdbcUrl = TestDataSource.dbModule.jdbcUrl),
            obo = Configuration.OBO(url = "OBO-url"),
            pdl = PdlMock.defaultConfiguration,
            auth = oAuthMock.authConfig,
            sykepengesoknadBackend =
                Configuration.SykepengesoknadBackend(
                    hostname = "sykepengesoknad-backend",
                    scope = OAuthScope("sykepengesoknad-backend-scope"),
                ),
            aareg = AARegMock.defaultConfiguration,
            ainntekt = AInntektMock.defaultConfiguration,
            ereg = EregMock.defaultConfiguration,
            inntektsmelding = InntektsmeldingApiMock.defaultConfiguration,
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
    pdlClient: PdlClient = PdlMock.pdlClient(),
    resetDatabase: Boolean = true,
    sykepengesoknadBackendClient: SykepengesoknadBackendClient = SykepengesoknadMock.sykepengersoknadBackendClientMock(oboClient = TestOppsett.oboClient),
    aaRegClient: AARegClient = AARegMock.aaRegClientMock(),
    aInntektClient: AInntektClient =
        AInntektMock.aInntektClientMock(fnrTilInntektApiUt = emptyMap()),
    eregClient: EregClient = EregMock.eregClientMock(),
    sigrunClient: SigrunClient = SigrunMock.sigrunMockClient(),
    inntektsmeldingClient: InntektsmeldingClient = InntektsmeldingApiMock.inntektsmeldingClientMock(),
    testBlock: suspend ApplicationTestBuilder.(daoer: Daoer) -> Unit,
) = testApplication {
    if (resetDatabase) {
        TestDataSource.resetDatasource()
    }
    application {
        val clienter =
            Clienter(
                pdlClient = pdlClient,
                sykepengesoknadBackendClient = sykepengesoknadBackendClient,
                aInntektClient = aInntektClient,
                aaRegClient = aaRegClient,
                eregClient = eregClient,
                inntektsmeldingClient = inntektsmeldingClient,
                sigrunClient = sigrunClient,
            )
        val services =
            createServices(
                clienter = clienter,
                db = skapDbDaoer(dataSource),
            )

        settOppKtor(dataSource, config, clienter, services) { services ->
            setupApiRoutes(services)
        }
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
internal fun instansierDatabase(configuration: Configuration.DB) = DBModule(configuration = configuration, ::testHikariConfigurator).also { it.migrate() }.dataSource

private fun testHikariConfigurator(configuration: Configuration.DB) =
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
