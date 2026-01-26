package no.nav.helse.bakrommet.e2e.behandling

import no.nav.helse.bakrommet.api.dto.tidslinje.TidslinjeBehandlingStatus
import no.nav.helse.bakrommet.domain.person.NaturligIdent
import no.nav.helse.bakrommet.e2e.TestOppsett.oAuthMock
import no.nav.helse.bakrommet.e2e.runApplicationTest
import no.nav.helse.bakrommet.e2e.testutils.ApiResult
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SaksbehandlingsperiodeStatusTest {
    private companion object {
        const val FNR = "01019012349"
    }

    @Test
    fun `diverse statusendringer på saksbehandlingsperiode`() =
        runApplicationTest {
            val personPseudoId = personsøk(NaturligIdent(FNR))

            val tokenSaksbehandler = oAuthMock.token(navIdent = "S111111", grupper = listOf("GRUPPE_SAKSBEHANDLER"))
            val tokenSaksbehandler2 = oAuthMock.token(navIdent = "S222222", grupper = listOf("GRUPPE_SAKSBEHANDLER"))
            val tokenBeslutter = oAuthMock.token(navIdent = "B111111", grupper = listOf("GRUPPE_BESLUTTER"))
            val tokenBeslutter2 = oAuthMock.token(navIdent = "B222222", grupper = listOf("GRUPPE_BESLUTTER"))

            // Opprett saksbehandlingsperiode via action
            val periodeOpprinnelig =
                opprettBehandling(
                    personPseudoId.toString(),
                    LocalDate.parse("2023-01-01"),
                    LocalDate.parse("2023-01-31"),
                    token = tokenSaksbehandler,
                )

            assertEquals(
                TidslinjeBehandlingStatus.UNDER_BEHANDLING,
                periodeOpprinnelig.status,
                "Status skal være UNDER_BEHANDLING for nyopprettet saksbehandlingsperiode",
            )

            // Test that saksbehandler #2 cannot send to beslutning (forbidden)
            val sendTilBeslutningResultForbidden =
                sendTilBeslutning(
                    personPseudoId,
                    periodeOpprinnelig.id,
                    tokenSaksbehandler2,
                    "En begrunnelse",
                )
            check(sendTilBeslutningResultForbidden is ApiResult.Error) {
                "Saksbehandler #2 skal ikke ha tilgang"
            }
            assertEquals(
                403,
                sendTilBeslutningResultForbidden.problemDetails.status,
                "Saksbehandler #2 er ikke saksbehandler på denne",
            )

            // Send til beslutning via action
            sendTilBeslutningOld(
                personPseudoId,
                periodeOpprinnelig.id,
                tokenSaksbehandler,
                "En begrunnelse",
            )

            // Ta til beslutning via action
            val periodeUnderBeslutning = taTilBeslutningOld(personPseudoId, periodeOpprinnelig.id, tokenBeslutter)
            assertEquals(
                periodeOpprinnelig
                    .copy(
                        status = TidslinjeBehandlingStatus.UNDER_BESLUTNING,
                        individuellBegrunnelse = "En begrunnelse",
                        beslutterNavIdent = "B111111",
                    ),
                periodeUnderBeslutning,
            )

            // Test error scenarios for sendTilbake - malformed request (missing kommentar field)
            sendTilbakeRaw(
                personPseudoId,
                periodeOpprinnelig.id,
                tokenBeslutter,
                body = """{ "mangler" : "kommentar-felt" }""",
            ).let { response ->
                assertEquals(400, response.status.value, "POST-body må inneholde kommentar-felt")
            }

            // Test error scenarios for sendTilbake - missing body/content-type
            sendTilbakeRaw(
                personPseudoId,
                periodeOpprinnelig.id,
                tokenBeslutter,
                body = null,
                setContentType = false,
            ).let { response ->
                assertEquals(415, response.status.value, "Mangler POST-body som application/json")
            }

            // Send tilbake via action
            val periode =
                sendTilbakeOld(
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

            // Send til beslutning again with new begrunnelse
            val periodeGjenTilBeslutning =
                sendTilBeslutningOld(
                    personPseudoId,
                    periodeOpprinnelig.id,
                    tokenSaksbehandler,
                    "En ny begrunnelse",
                )
            assertEquals(
                periodeOpprinnelig
                    .copy(
                        status = TidslinjeBehandlingStatus.UNDER_BESLUTNING,
                        individuellBegrunnelse = "En ny begrunnelse",
                        beslutterNavIdent = "B111111",
                    ),
                periodeGjenTilBeslutning,
                "Skal nå gå tilbake til opprinnelig beslutter, og ikke legges i beslutterkø",
            )

            // Test that beslutter #2 cannot godkjenn (forbidden)
            val godkjennResultForbidden = godkjenn(personPseudoId, periodeOpprinnelig.id, tokenBeslutter2)
            check(godkjennResultForbidden is ApiResult.Error) {
                "Beslutter #2 skal ikke ha tilgang"
            }
            assertEquals(
                403,
                godkjennResultForbidden.problemDetails.status,
                "Beslutter #2 er ikke beslutter på denne",
            )

            // Godkjenn via action
            val periodeGodkjent = godkjennOld(personPseudoId, periodeOpprinnelig.id, tokenBeslutter)
            assertEquals(
                periodeOpprinnelig
                    .copy(
                        status = TidslinjeBehandlingStatus.GODKJENT,
                        individuellBegrunnelse = "En ny begrunnelse",
                        beslutterNavIdent = "B111111",
                    ),
                periodeGodkjent,
            )

            // Verify historikk
            val historikk = hentHistorikk(personPseudoId, periodeOpprinnelig.id, tokenSaksbehandler)
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
