package no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.api.dto.dokumenter.DokumentDto
import no.nav.helse.bakrommet.e2e.TestOppsett
import org.junit.jupiter.api.Assertions
import java.util.UUID

suspend fun ApplicationTestBuilder.hentDokumenter(
    personPseudoId: UUID,
    behandlingId: UUID,
): List<DokumentDto> =
    client
        .get(
            "/v1/$personPseudoId/behandlinger/$behandlingId/dokumenter",
        ) {
            bearerAuth(TestOppsett.userToken)
        }.let { getResponse ->
            Assertions.assertEquals(200, getResponse.status.value)
            getResponse.body()
        }
