package no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagResponse
import no.nav.helse.bakrommet.serde.objectMapperCustomSerde
import java.util.UUID

internal suspend fun ApplicationTestBuilder.hentSykepengegrunnlag(
    personId: String,
    periodeId: UUID,
): SykepengegrunnlagResponse {
    val response =
        client.get("/v2/$personId/saksbehandlingsperioder/$periodeId/sykepengegrunnlag") {
            bearerAuth(TestOppsett.userToken)
        }

    val sykepengegrunnlagStr = response.body<String>()
    return objectMapperCustomSerde.readValue(sykepengegrunnlagStr, SykepengegrunnlagResponse::class.java)
}
