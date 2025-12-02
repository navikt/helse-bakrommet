package no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.Refusjonsperiode
import no.nav.helse.bakrommet.util.serialisertTilString
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.UUID

internal suspend fun ApplicationTestBuilder.settRefusjon(
    personId: String,
    periodeId: UUID,
    yrkesaktivitetId: UUID,
    refusjon: List<Refusjonsperiode>,
) {
    val response =
        client.put("/v1/$personId/behandlinger/$periodeId/yrkesaktivitet/$yrkesaktivitetId/refusjon") {
            bearerAuth(TestOppsett.userToken)
            contentType(ContentType.Application.Json)
            setBody(refusjon.serialisertTilString())
        }
    assertEquals(204, response.status.value)
}
