package no.nav.helse.bakrommet

import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.aareg.AARegMock
import no.nav.helse.bakrommet.ainntekt.AInntektMock
import no.nav.helse.bakrommet.auth.OAuthScope
import no.nav.helse.bakrommet.auth.OboClient
import no.nav.helse.bakrommet.errorhandling.installErrorHandling
import no.nav.helse.bakrommet.fakedaos.*
import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingApiMock
import no.nav.helse.bakrommet.pdl.PdlMock
import no.nav.helse.bakrommet.sigrun.SigrunMock
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadBackendClient
import no.nav.helse.bakrommet.util.sikkerLogger
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

// App-oppstarten må definere egen logger her, siden den (per nå) ikke skjer inne i en klasse
val appLogger: Logger = LoggerFactory.getLogger("bakrommet")

object FakeDaoer : AlleDaoer {
    override val saksbehandlingsperiodeDao = SaksbehandlingsperiodeDaoFake()
    override val saksbehandlingsperiodeEndringerDao = SaksbehandlingsperiodeEndringerDaoFake()
    override val personDao = PersonDaoFake()
    override val dokumentDao = DokumentDaoFake()
    override val yrkesaktivitetDao = YrkesaktivitetDaoFake()
    override val vurdertVilkårDao = VurdertVilkårDaoFake()
    override val sykepengegrunnlagDao = SykepengegrunnlagDaoFake()
    override val beregningDao = UtbetalingsberegningDaoFake()
    override val outboxDao = OutboxDaoFake()
}

fun main() {
    embeddedServer(CIO, port = 8080) {
        appLogger.info("Setter opp ktor")

        helsesjekker()
/*
        install(ContentNegotiation) {
            register(ContentType.Application.Json, JacksonConverter(objectMapper))
        }

 */
        install(CallLogging) {
            disableDefaultColors()
            logger = sikkerLogger
            level = Level.INFO
            filter { call -> call.request.path().let { it != "/isalive" && it != "/isready" } }
        }

        installErrorHandling(true)

        // Sett opp mock-klienter tilsvarende e2e-testene
        val oboClient: OboClient = PdlMock.createDefaultOboClient()

        val pdlClient = PdlMock.pdlClient(oboClient = oboClient)
        val aaRegClient = AARegMock.aaRegClientMock(oboClient = oboClient)
        val aInntektClient = AInntektMock.aInntektClientMock(oboClient = oboClient)
        val sigrunClient = SigrunMock.sigrunMockClient(oboClient = oboClient)
        val inntektsmeldingClient = InntektsmeldingApiMock.inntektsmeldingClientMock(oboClient = oboClient)

        // Enkel mock av sykepengesøknad-backend-klienten for demoformål
        val sykepengesoknadBackendClient =
            SykepengesoknadBackendClient(
                configuration =
                    Configuration.SykepengesoknadBackend(
                        hostname = "sykepengesoknad-backend",
                        scope = OAuthScope("sykepengesoknad-backend-scope"),
                    ),
                oboClient = oboClient,
            )

        TODO()
        /*
        val clienter =
            Clienter(
                pdlClient = pdlClient,
                sykepengesoknadBackendClient = sykepengesoknadBackendClient,
                aInntektClient = aInntektClient,
                aaRegClient = aaRegClient,
                inntektsmeldingClient = inntektsmeldingClient,
                sigrunClient = sigrunClient,
            )
        val services = createServices(clienter, FakeDaoer, FakeTransactionlfactory)
        routing {
            setupRoutes(services)
        }

        appLogger.info("Starter bakrommet")

         */
    }.start(true)
}
