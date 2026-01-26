package no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.api.dto.person.PersonsøkRequestDto
import no.nav.helse.bakrommet.api.dto.person.PersonsøkResponseDto
import no.nav.helse.bakrommet.domain.person.NaturligIdent
import no.nav.helse.bakrommet.serialisertTilString
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.*

internal suspend fun ApplicationTestBuilder.personsøk(
    naturligIdent: NaturligIdent,
    token: String = TestOppsett.userToken,
): UUID {
    val response =
        client.post("/v1/personsok") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(PersonsøkRequestDto(ident = naturligIdent.value).serialisertTilString())
        }

    assertEquals(200, response.status.value, "Personsøk skal returnere status 200")

    val responseBody = response.body<PersonsøkResponseDto>()
    return UUID.fromString(responseBody.personId)
}
