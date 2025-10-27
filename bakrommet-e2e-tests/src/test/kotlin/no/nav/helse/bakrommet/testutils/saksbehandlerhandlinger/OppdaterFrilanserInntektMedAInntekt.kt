package no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.FrilanserInntektRequest
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.InntektRequest
import no.nav.helse.bakrommet.serde.objectMapperCustomSerde
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.UUID

internal suspend fun ApplicationTestBuilder.oppdaterFrilanserInntektMedAInntekt(
    periodeId: UUID,
    yrkesaktivitetId: UUID,
    personId: String,
) {
    val inntektRequest =
        InntektRequest.Frilanser(
            data =
                FrilanserInntektRequest.Ainntekt(
                    begrunnelse = "Bruker a-inntekt for frilanser",
                ),
        )

    val response =
        client.put("/v1/$personId/saksbehandlingsperioder/$periodeId/yrkesaktivitet/$yrkesaktivitetId/inntekt") {
            bearerAuth(TestOppsett.userToken)
            contentType(ContentType.Application.Json)
            setBody(objectMapperCustomSerde.writeValueAsString(inntektRequest))
        }

    assertEquals(204, response.status.value, "Inntektsoppdatering for frilanser skal returnere status 204")
}
