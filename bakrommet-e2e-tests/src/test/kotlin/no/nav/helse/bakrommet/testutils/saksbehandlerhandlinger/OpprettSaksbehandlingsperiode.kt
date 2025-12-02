package no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.behandling.Behandling
import no.nav.helse.bakrommet.serde.objectMapperCustomSerde
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.time.LocalDate
import java.util.UUID

internal suspend fun ApplicationTestBuilder.opprettSaksbehandlingsperiode(
    personId: String,
    fom: LocalDate,
    tom: LocalDate,
    token: String = TestOppsett.userToken,
): Behandling {
    val response =
        client.post("/v1/$personId/behandlinger") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody("""{ "fom": "$fom", "tom": "$tom" }""")
        }

    assertEquals(201, response.status.value, "Saksbehandlingsperiode skal opprettes med status 201")

    val responseBody = response.body<JsonNode>()
    assertTrue(responseBody.has("id"), "Response skal inneholde periode ID")
    val periodeId = UUID.fromString(responseBody["id"].asText())
    assertTrue(periodeId != null, "Periode ID skal være gyldig UUID")

    val getResponse =
        client.get("/v1/$personId/behandlinger") {
            bearerAuth(TestOppsett.userToken)
        }

    assertEquals(200, getResponse.status.value, "Henting av saksbehandlingsperioder skal returnere status 200")

    val json = getResponse.body<String>()
    val perioder = objectMapperCustomSerde.readValue<List<Behandling>>(json, objectMapperCustomSerde.typeFactory.constructCollectionType(List::class.java, Behandling::class.java))

    assertTrue(perioder.isNotEmpty(), "Det skal finnes minst én saksbehandlingsperiode")
    val periode = perioder.first { it.id == periodeId }

    return periode
}
