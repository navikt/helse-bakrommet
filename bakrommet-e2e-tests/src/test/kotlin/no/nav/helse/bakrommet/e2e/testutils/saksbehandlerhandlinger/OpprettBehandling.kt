package no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.api.dto.behandling.BehandlingDto
import no.nav.helse.bakrommet.api.dto.behandling.OpprettBehandlingRequestDto
import no.nav.helse.bakrommet.e2e.TestOppsett
import no.nav.helse.bakrommet.objectMapper
import no.nav.helse.bakrommet.serialisertTilString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.time.LocalDate
import java.util.UUID

internal suspend fun ApplicationTestBuilder.opprettBehandling(
    personId: UUID,
    req: OpprettBehandlingRequestDto,
    token: String = TestOppsett.userToken,
): BehandlingDto {
    val response =
        client.post("/v1/$personId/behandlinger") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(req.serialisertTilString())
        }

    assertEquals(201, response.status.value, "BehandlingDto skal opprettes med status 201")

    val responseBody = response.body<JsonNode>()
    assertTrue(responseBody.has("id"), "Response skal inneholde ID")
    val behandlingId = UUID.fromString(responseBody["id"].asText())
    assertTrue(behandlingId != null, "Periode ID skal være gyldig UUID")

    val getResponse =
        client.get("/v1/$personId/behandlinger") {
            bearerAuth(TestOppsett.userToken)
        }

    assertEquals(200, getResponse.status.value, "Henting av behandling skal returnere status 200")

    val json = getResponse.body<String>()
    val perioder = objectMapper.readValue<List<BehandlingDto>>(json, objectMapper.typeFactory.constructCollectionType(List::class.java, BehandlingDto::class.java))

    assertTrue(perioder.isNotEmpty(), "Det skal finnes minst én behandling")
    val periode = perioder.first { it.id == behandlingId }

    return periode
}

internal suspend fun ApplicationTestBuilder.opprettBehandling(
    personId: String,
    fom: LocalDate,
    tom: LocalDate,
    token: String = TestOppsett.userToken,
): BehandlingDto {
    val req = OpprettBehandlingRequestDto(fom = fom, tom = tom, søknader = null)
    return opprettBehandling(UUID.fromString(personId), req, token)
}

internal suspend fun ApplicationTestBuilder.opprettBehandling(
    personId: UUID,
    fom: LocalDate,
    tom: LocalDate,
    token: String = TestOppsett.userToken,
): BehandlingDto {
    val req = OpprettBehandlingRequestDto(fom = fom, tom = tom, søknader = null)
    return opprettBehandling(personId, req, token)
}
