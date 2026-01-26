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
import no.nav.helse.bakrommet.e2e.testutils.ApiResult
import no.nav.helse.bakrommet.e2e.testutils.result
import no.nav.helse.bakrommet.objectMapper
import no.nav.helse.bakrommet.serialisertTilString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.time.LocalDate
import java.util.UUID

/**
 * Opprett behandling som returnerer BehandlingDto direkte og asserter på 201 status.
 * Bruk opprettBehandlingResult hvis du trenger å håndtere feilsituasjoner.
 */
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

/**
 * Opprett behandling som returnerer ApiResult for å kunne teste feilsituasjoner.
 */
internal suspend fun ApplicationTestBuilder.opprettBehandlingResult(
    personId: UUID,
    req: OpprettBehandlingRequestDto,
    token: String = TestOppsett.userToken,
): ApiResult<BehandlingDto> =
    client
        .post("/v1/$personId/behandlinger") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(req.serialisertTilString())
        }.let {
            it.result<BehandlingDto> {
                assertEquals(201, it.status.value, "BehandlingDto skal opprettes med status 201")
            }
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
