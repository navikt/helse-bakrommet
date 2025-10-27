package no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dag
import no.nav.helse.bakrommet.util.serialisertTilString
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.UUID

internal suspend fun ApplicationTestBuilder.settDagoversikt(
    periodeId: UUID,
    yrkesaktivitetId: UUID,
    personId: String,
    dager: List<Dag>,
) {
    val dagoversikt = dager.serialisertTilString()
    val response =
        client.put("/v1/$personId/saksbehandlingsperioder/$periodeId/yrkesaktivitet/$yrkesaktivitetId/dagoversikt") {
            bearerAuth(TestOppsett.userToken)
            contentType(ContentType.Application.Json)
            setBody(dagoversikt)
        }
    assertEquals(204, response.status.value)
}
