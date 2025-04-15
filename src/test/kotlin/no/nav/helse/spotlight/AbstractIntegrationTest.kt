package no.nav.helse.spotlight

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import kotliquery.sessionOf
import no.nav.helse.spotlight.db.SqlRunner
import no.nav.helse.spotlight.db.SuspendertKommandokjedeDao
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.time.Instant
import java.util.*

abstract class AbstractIntegrationTest {
    private val session = sessionOf(TestDatabase.dataSource, strict = true)
    protected val dao = SuspendertKommandokjedeDao(SqlRunner(session))

    protected val wireMockSlack: WireMockServer =
        WireMockServer(options().dynamicPort()).also(WireMockServer::start)

    protected val testRapid = TestRapid()

    protected val slackConfiguration =
        Configuration.Slack(
            accessToken = "mockAccessToken",
            channel = "mockChannel",
            url = wireMockSlack.baseUrl(),
        )

    @BeforeEach
    fun setUp() {
        start(
            dataSource = TestDatabase.dataSource,
            rapidsConnection = testRapid,
            slackConfiguration = slackConfiguration,
        )
    }

    @AfterEach
    fun tearDown() {
        dao.finnAlle()
            .map(SuspendertKommandokjede::commandContextId)
            .forEach(dao::slett)
        session.close()
        wireMockSlack.stop()
    }

    protected fun lagretKommandokjede(
        mottattTidspunkt: Instant = Instant.now().minusMinutes(5),
        sistePartisjonsnøkkel: String = "${UUID.randomUUID()}",
        antallGangerPåminnet: Int = 0,
        sti: String = "[ 0 ]",
    ): SuspendertKommandokjede =
        SuspendertKommandokjede(
            commandContextId = UUID.randomUUID(),
            command = "EnCommand",
            førsteTidspunkt = mottattTidspunkt,
            sisteTidspunkt = mottattTidspunkt,
            sisteMeldingId = UUID.randomUUID(),
            sistePartisjonsnøkkel = sistePartisjonsnøkkel,
            totaltAntallGangerPåminnet = antallGangerPåminnet,
            sistSuspenderteSti =
                SuspendertKommandokjede.Sti(
                    sti = sti,
                    førsteTidspunkt = mottattTidspunkt,
                    antallGangerPåminnet = antallGangerPåminnet,
                ),
        ).also(dao::insert).commandContextId.let(dao::finn).let(::checkNotNull)

    protected fun Instant.minusMinutes(minutes: Long): Instant = minusSeconds(minutes * 60)
}
