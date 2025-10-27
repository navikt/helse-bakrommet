package no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.util.somListe
import org.junit.jupiter.api.Assertions.assertEquals

internal suspend fun ApplicationTestBuilder.hentAllePerioder(
    personId: String,
): List<Saksbehandlingsperiode> {
    val response =
        client.get("/v1/$personId/saksbehandlingsperioder") {
            bearerAuth(TestOppsett.userToken)
        }

    assertEquals(200, response.status.value, "Henting av alle perioder skal returnere status 200")
    val perioder: List<Saksbehandlingsperiode> = response.bodyAsText().somListe()
    return perioder
}
