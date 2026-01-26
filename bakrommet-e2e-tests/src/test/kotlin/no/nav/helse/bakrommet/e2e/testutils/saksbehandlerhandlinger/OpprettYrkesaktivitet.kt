package no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.YrkesaktivitetCreateRequestDto
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.YrkesaktivitetDto
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.YrkesaktivitetKategoriseringDto
import no.nav.helse.bakrommet.e2e.TestOppsett
import no.nav.helse.bakrommet.e2e.testutils.ApiResult
import no.nav.helse.bakrommet.e2e.testutils.result
import no.nav.helse.bakrommet.serialisertTilString
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.*

internal suspend fun ApplicationTestBuilder.opprettYrkesaktivitetOld(
    personId: UUID,
    behandlingId: UUID,
    kategorisering: YrkesaktivitetKategoriseringDto,
): UUID {
    val result = opprettYrkesaktivitet(personId, behandlingId, kategorisering)
    check(result is ApiResult.Success)
    return result.response.id
}

internal suspend fun ApplicationTestBuilder.opprettYrkesaktivitetOgForventOk(
    personId: UUID,
    behandlingId: UUID,
    kategorisering: YrkesaktivitetKategoriseringDto,
): YrkesaktivitetDto {
    val result = opprettYrkesaktivitet(personId, behandlingId, kategorisering)
    check(result is ApiResult.Success<YrkesaktivitetDto>)
    return result.response
}

internal suspend fun ApplicationTestBuilder.opprettYrkesaktivitet(
    personId: UUID,
    behandlingId: UUID,
    kategorisering: YrkesaktivitetKategoriseringDto,
): ApiResult<YrkesaktivitetDto> =
    client
        .post("/v1/$personId/behandlinger/$behandlingId/yrkesaktivitet") {
            bearerAuth(TestOppsett.userToken)
            contentType(ContentType.Application.Json)
            setBody(YrkesaktivitetCreateRequestDto(kategorisering).serialisertTilString())
        }.let {
            it.result<YrkesaktivitetDto> {
                assertEquals(201, it.status.value, "Yrkesaktivitet skal opprettes med status 201")
            }
        }
