package no.nav.helse.bakrommet

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.behandling.Behandling
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.text.trimIndent

suspend fun ApplicationTestBuilder.sendTilBeslutning(
    behandling: Behandling,
    token: String = TestOppsett.userToken,
) {
    val response =
        this.client.post(
            "/v1/${behandling.naturligIdent}/behandlinger/${behandling.id}/sendtilbeslutning",
        ) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody("""{ "individuellBegrunnelse" : "En ny begrunnelse" }""".trimIndent())
        }
    assertEquals(200, response.status.value)
}

suspend fun ApplicationTestBuilder.taTilBesluting(
    behandling: Behandling,
    token: String,
) {
    val response =
        this.client.post(
            "/v1/${behandling.naturligIdent}/behandlinger/${behandling.id}/tatilbeslutning",
        ) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
        }
    assertEquals(200, response.status.value)
}
