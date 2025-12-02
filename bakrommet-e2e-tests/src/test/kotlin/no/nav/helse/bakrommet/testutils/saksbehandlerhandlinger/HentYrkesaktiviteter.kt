package no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetDTO
import no.nav.helse.bakrommet.serde.objectMapperCustomSerde
import java.util.UUID

internal suspend fun ApplicationTestBuilder.hentYrkesaktiviteter(
    personId: String,
    periodeId: UUID,
): List<YrkesaktivitetDTO> {
    val response =
        client
            .get("/v1/$personId/behandlinger/$periodeId/yrkesaktivitet") {
                bearerAuth(TestOppsett.userToken)
            }
    val json = response.body<String>()
    return objectMapperCustomSerde.readValue(json, objectMapperCustomSerde.typeFactory.constructCollectionType(List::class.java, YrkesaktivitetDTO::class.java))
}
