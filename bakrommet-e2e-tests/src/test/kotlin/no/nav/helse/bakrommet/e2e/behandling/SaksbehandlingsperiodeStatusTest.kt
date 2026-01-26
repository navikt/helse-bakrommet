package no.nav.helse.bakrommet.e2e.behandling

import no.nav.helse.bakrommet.api.dto.tidslinje.TidslinjeBehandlingStatus
import no.nav.helse.bakrommet.domain.enNaturligIdent
import no.nav.helse.bakrommet.domain.enNavIdent
import no.nav.helse.bakrommet.e2e.TestOppsett.oAuthMock
import no.nav.helse.bakrommet.e2e.runApplicationTest
import no.nav.helse.bakrommet.e2e.testutils.ApiResult
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.*
import no.nav.helse.bakrommet.e2e.testutils.truncateTidspunkt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertIs

class SaksbehandlingsperiodeStatusTest {
    private val naturligIdent = enNaturligIdent()

    @Test
    fun `diverse statusendringer på saksbehandlingsperiode`() =
        runApplicationTest {
            val personPseudoId = personsøk(naturligIdent)

            val saksbehandler1Ident = enNavIdent()
            val saksbehandler2Ident = enNavIdent()
            val beslutter1Ident = enNavIdent()
            val beslutter2Ident = enNavIdent()
            val tokenSaksbehandler = oAuthMock.token(navIdent = saksbehandler1Ident, grupper = listOf("GRUPPE_SAKSBEHANDLER"))
            val tokenSaksbehandler2 = oAuthMock.token(navIdent = saksbehandler2Ident, grupper = listOf("GRUPPE_SAKSBEHANDLER"))
            val tokenBeslutter = oAuthMock.token(navIdent = beslutter1Ident, grupper = listOf("GRUPPE_BESLUTTER"))
            val tokenBeslutter2 = oAuthMock.token(navIdent = beslutter2Ident, grupper = listOf("GRUPPE_BESLUTTER"))

            // Opprett saksbehandlingsperiode via action
            val opprinneligBehandling =
                opprettBehandlingOgForventOk(
                    personPseudoId,
                    LocalDate.parse("2023-01-01"),
                    LocalDate.parse("2023-01-31"),
                    token = tokenSaksbehandler,
                )

            assertEquals(
                TidslinjeBehandlingStatus.UNDER_BEHANDLING,
                opprinneligBehandling.status,
                "Status skal være UNDER_BEHANDLING for nyopprettet saksbehandlingsperiode",
            )

            // Test that saksbehandler #2 cannot send to beslutning (forbidden)
            val sendTilBeslutningResultForbidden =
                sendTilBeslutning(
                    personPseudoId,
                    opprinneligBehandling.id,
                    tokenSaksbehandler2,
                    "En begrunnelse",
                )
            assertIs<ApiResult.Error>(sendTilBeslutningResultForbidden, "Saksbehandler #2 skal ikke ha tilgang")
            assertEquals(
                403,
                sendTilBeslutningResultForbidden.problemDetails.status,
                "Saksbehandler #2 er ikke saksbehandler på denne",
            )

            // Send til beslutning via action
            sendTilBeslutningOgForventOk(
                personPseudoId,
                opprinneligBehandling.id,
                tokenSaksbehandler,
                "En begrunnelse",
            )

            // Ta til beslutning via action
            val behandlingUnderBeslutning = taTilBeslutningOgForventOk(personPseudoId, opprinneligBehandling.id, tokenBeslutter)
            assertEquals(
                opprinneligBehandling
                    .copy(
                        status = TidslinjeBehandlingStatus.UNDER_BESLUTNING,
                        individuellBegrunnelse = "En begrunnelse",
                        beslutterNavIdent = beslutter1Ident,
                    ).truncateTidspunkt(),
                behandlingUnderBeslutning.truncateTidspunkt(),
            )

            // Test error scenarios for sendTilbake - malformed request (missing kommentar field)
            sendTilbakeRaw(
                personPseudoId,
                opprinneligBehandling.id,
                tokenBeslutter,
                body = """{ "mangler" : "kommentar-felt" }""",
            ).let { response ->
                assertEquals(400, response.status.value, "POST-body må inneholde kommentar-felt")
            }

            // Test error scenarios for sendTilbake - missing body/content-type
            sendTilbakeRaw(
                personPseudoId,
                opprinneligBehandling.id,
                tokenBeslutter,
                body = null,
                setContentType = false,
            ).let { response ->
                assertEquals(415, response.status.value, "Mangler POST-body som application/json")
            }

            // Send tilbake via action
            val behandling =
                sendTilbakeOgForventOk(
                    personPseudoId.toString(),
                    opprinneligBehandling.id,
                    tokenBeslutter,
                    "Dette blir litt feil",
                )
            assertEquals(
                opprinneligBehandling
                    .copy(
                        status = TidslinjeBehandlingStatus.UNDER_BEHANDLING,
                        individuellBegrunnelse = "En begrunnelse",
                        beslutterNavIdent = beslutter1Ident,
                    ).truncateTidspunkt(),
                behandling.truncateTidspunkt(),
                "Tilbake til 'under_behandling', men beslutter beholdes",
            )

            // Send til beslutning again with new begrunnelse
            val behandlingIgjenTilBeslutning =
                sendTilBeslutningOgForventOk(
                    personPseudoId,
                    opprinneligBehandling.id,
                    tokenSaksbehandler,
                    "En ny begrunnelse",
                )
            assertEquals(
                opprinneligBehandling
                    .copy(
                        status = TidslinjeBehandlingStatus.UNDER_BESLUTNING,
                        individuellBegrunnelse = "En ny begrunnelse",
                        beslutterNavIdent = beslutter1Ident,
                    ).truncateTidspunkt(),
                behandlingIgjenTilBeslutning.truncateTidspunkt(),
                "Skal nå gå tilbake til opprinnelig beslutter, og ikke legges i beslutterkø",
            )

            // Test that beslutter #2 cannot godkjenn (forbidden)
            val godkjennResultForbidden = godkjenn(personPseudoId, opprinneligBehandling.id, tokenBeslutter2)
            assertIs<ApiResult.Error>(godkjennResultForbidden, "Beslutter #2 skal ikke ha tilgang")
            assertEquals(
                403,
                godkjennResultForbidden.problemDetails.status,
                "Beslutter #2 er ikke beslutter på denne",
            )

            // Godkjenn via action
            val periodeGodkjent = godkjennOgForventOk(personPseudoId, opprinneligBehandling.id, tokenBeslutter)
            assertEquals(
                opprinneligBehandling
                    .copy(
                        status = TidslinjeBehandlingStatus.GODKJENT,
                        individuellBegrunnelse = "En ny begrunnelse",
                        beslutterNavIdent = beslutter1Ident,
                    ).truncateTidspunkt(),
                periodeGodkjent.truncateTidspunkt(),
            )

            // Verify historikk
            val historikk = hentHistorikk(personPseudoId, opprinneligBehandling.id, tokenSaksbehandler)
            assertEquals(
                listOf(
                    listOf("UNDER_BEHANDLING", "STARTET", saksbehandler1Ident, null),
                    listOf("TIL_BESLUTNING", "SENDT_TIL_BESLUTNING", saksbehandler1Ident, null),
                    listOf("UNDER_BESLUTNING", "TATT_TIL_BESLUTNING", beslutter1Ident, null),
                    listOf("UNDER_BEHANDLING", "SENDT_I_RETUR", beslutter1Ident, "Dette blir litt feil"),
                    listOf("UNDER_BESLUTNING", "SENDT_TIL_BESLUTNING", saksbehandler1Ident, null),
                    listOf("GODKJENT", "GODKJENT", beslutter1Ident, null),
                ),
                historikk.map { listOf(it.status.name, it.endringType.name, it.endretAvNavIdent, it.endringKommentar) },
            )
        }
}
