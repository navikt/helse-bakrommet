package no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.behandling.Saksbehandlingsperiode
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.UUID

internal suspend fun ApplicationTestBuilder.sendTilBeslutning(
    personId: String,
    periodeId: UUID,
    token: String = TestOppsett.userToken,
    individuellBegrunnelse: String = "En begrunnelse",
): Saksbehandlingsperiode {
    val response =
        client.post("/v1/$personId/saksbehandlingsperioder/$periodeId/sendtilbeslutning") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody("""{ "individuellBegrunnelse" : "$individuellBegrunnelse" }""")
        }

    assertEquals(200, response.status.value, "Send til beslutning skal returnere status 200")

    return response.body<Saksbehandlingsperiode>()
}
