package no.nav.helse.bakrommet.e2e.behandling

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import no.nav.helse.bakrommet.api.dto.behandling.BehandlingDto
import no.nav.helse.bakrommet.api.dto.tidslinje.TidslinjeBehandlingStatus
import no.nav.helse.bakrommet.behandling.SaksbehandlingsperiodeEndring
import no.nav.helse.bakrommet.domain.person.NaturligIdent
import no.nav.helse.bakrommet.e2e.TestOppsett.oAuthMock
import no.nav.helse.bakrommet.e2e.runApplicationTest
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.opprettBehandling
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.sendTilBeslutning
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.sendTilbake
import no.nav.helse.bakrommet.e2e.testutils.`should equal`
import no.nav.helse.bakrommet.somListe
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class SaksbehandlingsperiodeStatusTest {
    private companion object {
        val fnr = "01019012349"
        val personId = "65hth"
        val personPseudoId = UUID.nameUUIDFromBytes(personId.toByteArray())
    }

    @Test
    fun `diverse statusendringer på saksbehandlingsperiode`() =
        runApplicationTest {
            it.personPseudoIdDao.opprettPseudoId(personPseudoId, NaturligIdent(fnr))

            val tokenSaksbehandler = oAuthMock.token(navIdent = "S111111", grupper = listOf("GRUPPE_SAKSBEHANDLER"))
            val tokenSaksbehandler2 = oAuthMock.token(navIdent = "S222222", grupper = listOf("GRUPPE_SAKSBEHANDLER"))
            val tokenBeslutter = oAuthMock.token(navIdent = "B111111", grupper = listOf("GRUPPE_BESLUTTER"))
            val tokenBeslutter2 = oAuthMock.token(navIdent = "B222222", grupper = listOf("GRUPPE_BESLUTTER"))

            it.outboxDao.hentAlleUpubliserteEntries().size `should equal` 0

            // Opprett saksbehandlingsperiode via action
            val periodeOpprinnelig =
                opprettBehandling(
                    personPseudoId.toString(),
                    LocalDate.parse("2023-01-01"),
                    LocalDate.parse("2023-01-31"),
                    token = tokenSaksbehandler,
                )

            val outboxAfterCreation = it.outboxDao.hentAlleUpubliserteEntries()
            assertEquals(1, outboxAfterCreation.size, "Det skal være én melding i outbox etter opprettelse av perioden")

            assertEquals(
                TidslinjeBehandlingStatus.UNDER_BEHANDLING,
                periodeOpprinnelig.status,
                "Status skal være UNDER_BEHANDLING for nyopprettet saksbehandlingsperiode",
            )

            client
                .post("/v1/$personPseudoId/behandlinger/${periodeOpprinnelig.id}/sendtilbeslutning") {
                    bearerAuth(tokenSaksbehandler2)
                    contentType(ContentType.Application.Json)
                    setBody("""{ "individuellBegrunnelse" : "En begrunnelse" }""".trimIndent())
                }.let { response ->
                    assertEquals(
                        403,
                        response.status.value,
                        "Saksbehandler #2 er ikke saksbehandler på denne",
                    )
                }

            // Send til beslutning via action

            sendTilBeslutning(
                personPseudoId,
                periodeOpprinnelig.id,
                tokenSaksbehandler,
                "En begrunnelse",
            )

            client
                .post("/v1/$personPseudoId/behandlinger/${periodeOpprinnelig.id}/tatilbeslutning") {
                    bearerAuth(tokenBeslutter)
                }.let { response ->
                    assertEquals(200, response.status.value)
                    val periode = response.body<BehandlingDto>()
                    assertEquals(
                        periodeOpprinnelig
                            .copy(
                                status = TidslinjeBehandlingStatus.UNDER_BESLUTNING,
                                individuellBegrunnelse = "En begrunnelse",
                                beslutterNavIdent = "B111111",
                            ),
                        periode,
                    )
                }

            client
                .post("/v1/$personPseudoId/behandlinger/${periodeOpprinnelig.id}/sendtilbake") {
                    bearerAuth(tokenBeslutter)
                    contentType(ContentType.Application.Json)
                    setBody("""{ "mangler" : "kommentar-felt" }""")
                }.let { response ->
                    assertEquals(400, response.status.value, "POST-body må inneholde kommentar-felt")
                }

            client
                .post("/v1/$personPseudoId/behandlinger/${periodeOpprinnelig.id}/sendtilbake") {
                    bearerAuth(tokenBeslutter)
                }.let { response ->
                    assertEquals(415, response.status.value, "Mangler POST-body som application/json")
                }

            // Send tilbake via action
            val periode =
                sendTilbake(
                    personPseudoId.toString(),
                    periodeOpprinnelig.id,
                    tokenBeslutter,
                    "Dette blir litt feil",
                )
            assertEquals(
                periodeOpprinnelig
                    .copy(
                        status = TidslinjeBehandlingStatus.UNDER_BEHANDLING,
                        individuellBegrunnelse = "En begrunnelse",
                        beslutterNavIdent = "B111111",
                    ),
                periode,
                "Tilbake til 'under_behandling', men beslutter beholdes",
            )

            client
                .post("/v1/$personPseudoId/behandlinger/${periodeOpprinnelig.id}/sendtilbeslutning") {
                    bearerAuth(tokenSaksbehandler)
                    contentType(ContentType.Application.Json)
                    setBody("""{ "individuellBegrunnelse" : "En ny begrunnelse" }""".trimIndent())
                }.let { response ->
                    assertEquals(200, response.status.value)
                    val periode = response.body<BehandlingDto>()
                    assertEquals(
                        periodeOpprinnelig
                            .copy(
                                status = TidslinjeBehandlingStatus.UNDER_BESLUTNING,
                                individuellBegrunnelse = "En ny begrunnelse",
                                beslutterNavIdent = "B111111",
                            ),
                        periode,
                        "Skal nå gå tilbake til opprinnelig beslutter, og ikke legges i beslutterkø",
                    )
                }

            client
                .post("/v1/$personPseudoId/behandlinger/${periodeOpprinnelig.id}/godkjenn") {
                    bearerAuth(tokenBeslutter2)
                }.let { response ->
                    assertEquals(
                        403,
                        response.status.value,
                        "Beslutter #2 er ikke beslutter på denne",
                    )
                }

            client
                .post("/v1/$personPseudoId/behandlinger/${periodeOpprinnelig.id}/godkjenn") {
                    bearerAuth(tokenBeslutter)
                }.let { response ->
                    assertEquals(200, response.status.value)
                    val periode = response.body<BehandlingDto>()
                    assertEquals(
                        periodeOpprinnelig
                            .copy(
                                status = TidslinjeBehandlingStatus.GODKJENT,
                                individuellBegrunnelse = "En ny begrunnelse",
                                beslutterNavIdent = "B111111",
                            ),
                        periode,
                    )
                }

            val outboxAfterApproval = it.outboxDao.hentAlleUpubliserteEntries()
            assertEquals(3, outboxAfterApproval.size, "Det skal være 3 meldinger i outbox etter godkjenning av perioden")

            client
                .get("/v1/$personPseudoId/behandlinger/${periodeOpprinnelig.id}/historikk") {
                    bearerAuth(tokenSaksbehandler)
                }.let { response ->
                    val historikk = response.bodyAsText().somListe<SaksbehandlingsperiodeEndring>()

                    assertEquals(
                        listOf(
                            listOf("UNDER_BEHANDLING", "STARTET", "S111111", null),
                            listOf("TIL_BESLUTNING", "SENDT_TIL_BESLUTNING", "S111111", null),
                            listOf("UNDER_BESLUTNING", "TATT_TIL_BESLUTNING", "B111111", null),
                            listOf("UNDER_BEHANDLING", "SENDT_I_RETUR", "B111111", "Dette blir litt feil"),
                            listOf("UNDER_BESLUTNING", "SENDT_TIL_BESLUTNING", "S111111", null),
                            listOf("GODKJENT", "GODKJENT", "B111111", null),
                        ),
                        historikk.map { listOf(it.status.name, it.endringType.name, it.endretAvNavIdent, it.endringKommentar) },
                    )
                }
        }
}
