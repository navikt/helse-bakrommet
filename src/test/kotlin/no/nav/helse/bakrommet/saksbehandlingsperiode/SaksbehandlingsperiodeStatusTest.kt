package no.nav.helse.bakrommet.saksbehandlingsperiode

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import no.nav.helse.bakrommet.TestOppsett.oAuthMock
import no.nav.helse.bakrommet.runApplicationTest
import no.nav.helse.bakrommet.testutils.truncateTidspunkt
import no.nav.helse.bakrommet.util.somListe
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SaksbehandlingsperiodeStatusTest {
    private companion object {
        val fnr = "01019012349"
        val personId = "65hth"
    }

    @Test
    fun `diverse statusendringer på saksbehandlingsperiode`() =
        runApplicationTest {
            it.personDao.opprettPerson(fnr, personId)

            val tokenSaksbehandler = oAuthMock.token(navIdent = "S111111", grupper = listOf("GRUPPE_SAKSBEHANDLER"))
            val tokenSaksbehandler2 = oAuthMock.token(navIdent = "S222222", grupper = listOf("GRUPPE_SAKSBEHANDLER"))
            val tokenBeslutter = oAuthMock.token(navIdent = "B111111", grupper = listOf("GRUPPE_BESLUTTER"))
            val tokenBeslutter2 = oAuthMock.token(navIdent = "B222222", grupper = listOf("GRUPPE_BESLUTTER"))

            val createResponse =
                client.post("/v1/$personId/saksbehandlingsperioder") {
                    bearerAuth(tokenSaksbehandler)
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        { "fom": "2023-01-01", "tom": "2023-01-31" }
                        """.trimIndent(),
                    )
                }
            assertEquals(201, createResponse.status.value)

            val periodeOpprinnelig = createResponse.body<Saksbehandlingsperiode>().truncateTidspunkt()

            assertEquals(
                SaksbehandlingsperiodeStatus.UNDER_BEHANDLING,
                periodeOpprinnelig.status,
                "Status skal være UNDER_BEHANDLING for nyopprettet saksbehandlingsperiode",
            )

            client.post("/v1/$personId/saksbehandlingsperioder/${periodeOpprinnelig.id}/sendtilbeslutning") {
                bearerAuth(tokenSaksbehandler2)
            }.let { response ->
                assertEquals(
                    403,
                    response.status.value,
                    "Saksbehandler #2 er ikke saksbehandler på denne",
                )
            }

            client.post("/v1/$personId/saksbehandlingsperioder/${periodeOpprinnelig.id}/sendtilbeslutning") {
                bearerAuth(tokenSaksbehandler)
            }.let { response ->
                assertEquals(200, response.status.value)
                val periode = response.body<Saksbehandlingsperiode>()
                println(periode)
            }

            client.post("/v1/$personId/saksbehandlingsperioder/${periodeOpprinnelig.id}/tatilbeslutning") {
                bearerAuth(tokenBeslutter)
            }.let { response ->
                assertEquals(200, response.status.value)
                val periode = response.body<Saksbehandlingsperiode>()
                assertEquals(
                    periodeOpprinnelig.copy(
                        status = SaksbehandlingsperiodeStatus.UNDER_BESLUTNING,
                        beslutterNavIdent = "B111111",
                    ).truncateTidspunkt(),
                    periode.truncateTidspunkt(),
                )
            }

            client.post("/v1/$personId/saksbehandlingsperioder/${periodeOpprinnelig.id}/sendtilbake") {
                bearerAuth(tokenBeslutter)
            }.let { response ->
                assertEquals(200, response.status.value)
                val periode = response.body<Saksbehandlingsperiode>()
                assertEquals(
                    periodeOpprinnelig.copy(
                        status = SaksbehandlingsperiodeStatus.UNDER_BEHANDLING,
                        beslutterNavIdent = "B111111",
                    ).truncateTidspunkt(),
                    periode.truncateTidspunkt(),
                    "Tilbake til 'under_behandling', men beslutter beholdes",
                )
            }

            client.post("/v1/$personId/saksbehandlingsperioder/${periodeOpprinnelig.id}/sendtilbeslutning") {
                bearerAuth(tokenSaksbehandler)
            }.let { response ->
                assertEquals(200, response.status.value)
                val periode = response.body<Saksbehandlingsperiode>()
                assertEquals(
                    periodeOpprinnelig.copy(
                        status = SaksbehandlingsperiodeStatus.UNDER_BESLUTNING,
                        beslutterNavIdent = "B111111",
                    ).truncateTidspunkt(),
                    periode.truncateTidspunkt(),
                    "Skal nå gå tilbake til opprinnelig beslutter, og ikke legges i beslutterkø",
                )
            }

            client.post("/v1/$personId/saksbehandlingsperioder/${periodeOpprinnelig.id}/godkjenn") {
                bearerAuth(tokenBeslutter2)
            }.let { response ->
                assertEquals(
                    403,
                    response.status.value,
                    "Beslutter #2 er ikke beslutter på denne",
                )
            }

            client.post("/v1/$personId/saksbehandlingsperioder/${periodeOpprinnelig.id}/godkjenn") {
                bearerAuth(tokenBeslutter)
            }.let { response ->
                assertEquals(200, response.status.value)
                val periode = response.body<Saksbehandlingsperiode>()
                assertEquals(
                    periodeOpprinnelig.copy(
                        status = SaksbehandlingsperiodeStatus.GODKJENT,
                        beslutterNavIdent = "B111111",
                    ).truncateTidspunkt(),
                    periode.truncateTidspunkt(),
                )
            }

            client.get("/v1/$personId/saksbehandlingsperioder/${periodeOpprinnelig.id}/historikk") {
                bearerAuth(tokenSaksbehandler)
            }.let { response ->
                val historikk = response.bodyAsText().somListe<SaksbehandlingsperiodeEndring>()

                assertEquals(
                    listOf(
                        listOf("UNDER_BEHANDLING", "STARTET", "S111111"),
                        listOf("TIL_BESLUTNING", "SENDT_TIL_BESLUTNING", "S111111"),
                        listOf("UNDER_BESLUTNING", "TATT_TIL_BESLUTNING", "B111111"),
                        listOf("UNDER_BEHANDLING", "SENDT_I_RETUR", "B111111"),
                        listOf("UNDER_BESLUTNING", "SENDT_TIL_BESLUTNING", "S111111"),
                        listOf("GODKJENT", "GODKJENT", "B111111"),
                    ),
                    historikk.map { listOf(it.status.name, it.endringType.name, it.endretAvNavIdent) },
                )
            }
        }
}
