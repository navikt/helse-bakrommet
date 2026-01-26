package no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.api.dto.behandling.BehandlingDto
import no.nav.helse.bakrommet.e2e.TestOppsett
import no.nav.helse.bakrommet.somListe
import org.junit.jupiter.api.Assertions.assertEquals

internal suspend fun ApplicationTestBuilder.hentAlleBehandlinger(): List<BehandlingDto> {
    val response =
        client.get("/v1/behandlinger") {
            bearerAuth(TestOppsett.userToken)
        }

    assertEquals(200, response.status.value, "Henting av alle perioder skal returnere status 200")
    val perioder: List<BehandlingDto> = response.bodyAsText().somListe()
    return perioder
}
