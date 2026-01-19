package no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.server.testing.*
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.YrkesaktivitetDto
import no.nav.helse.bakrommet.util.objectMapper
import java.util.*

internal suspend fun ApplicationTestBuilder.hentYrkesaktiviteter(
    pseudoID: UUID,
    behandlingId: UUID,
): List<YrkesaktivitetDto> {
    val response =
        client
            .get("/v1/$pseudoID/behandlinger/$behandlingId/yrkesaktivitet") {
                bearerAuth(TestOppsett.userToken)
            }


    val json = response.body<String>()
    if(response.status.value !in 200..299) {
        error("Feil ved henting av yrkesaktiviteter: ${response.status.value} - $json")
    }
    return objectMapper.readValue<List<YrkesaktivitetDto>>(json)
}
