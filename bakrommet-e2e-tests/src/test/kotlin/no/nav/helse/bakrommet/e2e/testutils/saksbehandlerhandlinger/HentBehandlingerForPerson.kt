package no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.api.dto.behandling.BehandlingDto
import no.nav.helse.bakrommet.e2e.TestOppsett
import java.util.UUID

internal suspend fun ApplicationTestBuilder.hentBehandlingerForPerson(personPseudoId: UUID): List<BehandlingDto> =
    client
        .get("/v1/$personPseudoId/behandlinger") {
            bearerAuth(TestOppsett.userToken)
        }.body<List<BehandlingDto>>()
