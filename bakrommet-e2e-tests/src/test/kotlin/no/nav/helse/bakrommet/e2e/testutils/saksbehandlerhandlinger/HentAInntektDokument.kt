package no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.api.dto.dokumenter.DokumentDto
import no.nav.helse.bakrommet.e2e.TestOppsett
import no.nav.helse.bakrommet.e2e.testutils.ApiResult
import no.nav.helse.bakrommet.e2e.testutils.result
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.*

suspend fun ApplicationTestBuilder.hentAInntektDokument(
    personPseudoId: UUID,
    behandlingId: UUID,
): ApiResult<DokumentDto> =
    client
        .post("/v1/$personPseudoId/behandlinger/$behandlingId/dokumenter/ainntekt/hent-8-28") {
            bearerAuth(TestOppsett.userToken)
            contentType(ContentType.Application.Json)
        }.let { postResponse ->
            val result =
                postResponse.result<DokumentDto> {
                    assertEquals("ainntekt828", dokumentType)
                    assertEquals(201, postResponse.status.value)
                }

            return result
        }
