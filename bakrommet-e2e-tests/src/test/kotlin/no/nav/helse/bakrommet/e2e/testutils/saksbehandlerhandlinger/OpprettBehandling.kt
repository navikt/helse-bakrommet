package no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.api.dto.behandling.BehandlingDto
import no.nav.helse.bakrommet.api.dto.behandling.OpprettBehandlingRequestDto
import no.nav.helse.bakrommet.e2e.TestOppsett
import no.nav.helse.bakrommet.e2e.testutils.ApiResult
import no.nav.helse.bakrommet.e2e.testutils.result
import no.nav.helse.bakrommet.serialisertTilString
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.LocalDate
import java.util.*
import kotlin.test.assertIs

/**
 * Opprett behandling som returnerer BehandlingDto direkte og asserter på 201 status.
 * Bruk opprettBehandlingResult hvis du trenger å håndtere feilsituasjoner.
 */
internal suspend fun ApplicationTestBuilder.opprettBehandlingOgForventOk(
    personId: UUID,
    fom: LocalDate,
    tom: LocalDate,
    søknader: List<UUID>? = null,
    token: String = TestOppsett.userToken,
): BehandlingDto {
    val result = opprettBehandling(personId, fom, tom, søknader, token)
    assertIs<ApiResult.Success<BehandlingDto>>(result)
    return result.response
}

internal suspend fun ApplicationTestBuilder.opprettBehandling(
    personId: UUID,
    fom: LocalDate,
    tom: LocalDate,
    søknader: List<UUID>? = null,
    token: String = TestOppsett.userToken,
): ApiResult<BehandlingDto> {
    val req = OpprettBehandlingRequestDto(fom = fom, tom = tom, søknader = søknader)
    val response =
        client.post("/v1/$personId/behandlinger") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(req.serialisertTilString())
        }
    return response.result<BehandlingDto> {
        assertEquals(HttpStatusCode.Created, response.status)
    }
}
