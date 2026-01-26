package no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.api.dto.behandling.OppdaterSkjæringstidspunktRequestDto
import no.nav.helse.bakrommet.e2e.TestOppsett
import no.nav.helse.bakrommet.serialisertTilString
import java.time.LocalDate
import java.util.UUID

internal suspend fun ApplicationTestBuilder.oppdaterSkjæringstidspunkt(
    personPseudoId: UUID,
    behandlingId: UUID,
    dato: LocalDate,
) {
    val request = OppdaterSkjæringstidspunktRequestDto(dato.toString())
    client.put("/v1/$personPseudoId/behandlinger/$behandlingId/skjaeringstidspunkt") {
        bearerAuth(TestOppsett.userToken)
        contentType(ContentType.Application.Json)
        setBody(request.serialisertTilString())
    }
}
