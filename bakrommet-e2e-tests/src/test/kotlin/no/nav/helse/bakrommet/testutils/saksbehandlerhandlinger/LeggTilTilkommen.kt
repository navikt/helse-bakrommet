package no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.behandling.tilkommen.OpprettTilkommenInntektRequest
import no.nav.helse.bakrommet.behandling.tilkommen.TilkommenInntektResponse
import no.nav.helse.bakrommet.util.objectMapper
import no.nav.helse.bakrommet.util.serialisertTilString
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.UUID

internal suspend fun ApplicationTestBuilder.leggTilTilkommenInntekt(
    personId: String,
    periodeId: UUID,
    tilkommenInntekt: OpprettTilkommenInntektRequest,
): TilkommenInntektResponse {
    val response =
        client.post("/v1/$personId/saksbehandlingsperioder/$periodeId/tilkommeninntekt") {
            bearerAuth(TestOppsett.userToken)
            contentType(ContentType.Application.Json)
            setBody(tilkommenInntekt.serialisertTilString())
        }
    assertEquals(201, response.status.value)
    return objectMapper.readValue(response.bodyAsText())
}
