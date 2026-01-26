package no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.api.dto.dokumenter.DokumentDto
import no.nav.helse.bakrommet.e2e.TestOppsett
import no.nav.helse.bakrommet.e2e.testutils.ApiResult
import no.nav.helse.bakrommet.e2e.testutils.result
import org.junit.jupiter.api.Assertions
import java.util.*

internal suspend fun ApplicationTestBuilder.hentArbeidsforholdDokument(
    personPseudoId: UUID?,
    behandlingId: UUID,
): ApiResult<DokumentDto> {
    client
        .post("/v1/$personPseudoId/behandlinger/$behandlingId/dokumenter/arbeidsforhold/hent") {
            bearerAuth(TestOppsett.userToken)
        }.let { postResponse ->
            return postResponse.result<DokumentDto> {
                Assertions.assertEquals("arbeidsforhold", dokumentType)
                Assertions.assertEquals(201, postResponse.status.value)
            }
        }
}
