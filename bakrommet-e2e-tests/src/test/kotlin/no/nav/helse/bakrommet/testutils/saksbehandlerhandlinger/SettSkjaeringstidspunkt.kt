package no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.TestOppsett
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.LocalDate
import java.util.UUID

internal suspend fun ApplicationTestBuilder.settSkjaeringstidspunkt(
    personId: String,
    behandlingId: UUID,
    skjaeringstidspunkt: LocalDate,
) {
    val response =
        client.put("/v1/$personId/behandlinger/$behandlingId/skjaeringstidspunkt") {
            bearerAuth(TestOppsett.userToken)
            contentType(ContentType.Application.Json)
            setBody("""{ "skjaeringstidspunkt": "$skjaeringstidspunkt" }""")
        }

    assertEquals(200, response.status.value, "Skjæringstidspunkt skal settes med status 200")
}
