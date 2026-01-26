package no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.ArbeidstakerInntektRequestDto
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.ArbeidstakerSkjønnsfastsettelseÅrsakDto
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.InntektRequestDto
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.RefusjonsperiodeDto
import no.nav.helse.bakrommet.e2e.TestOppsett
import no.nav.helse.bakrommet.e2e.testutils.ApiResult
import no.nav.helse.bakrommet.e2e.testutils.result
import no.nav.helse.bakrommet.serialisertTilString
import java.util.*
import kotlin.test.assertEquals

suspend fun ApplicationTestBuilder.saksbehandlerVelgerInntekt(
    personPseudoId: UUID,
    behandlingId: UUID,
    yrkesaktivitetId: UUID,
    inntektskilde: InntektRequestDto,
): ApiResult<Unit> =
    client
        .put("/v1/$personPseudoId/behandlinger/$behandlingId/yrkesaktivitet/$yrkesaktivitetId/inntekt") {
            bearerAuth(TestOppsett.userToken)
            contentType(ContentType.Application.Json)
            setBody(inntektskilde.serialisertTilString())
        }.let {
            it.result {
                assertEquals(HttpStatusCode.NoContent, it.status)
            }
        }

suspend fun ApplicationTestBuilder.saksbehandlerVelgerInntektsmelding(
    personPseudoId: UUID,
    behandlingId: UUID,
    yrkesaktivitetId: UUID,
    inntektsmeldingId: String,
    begrunnelse: String? = null,
    refusjonsperiodeDto: List<RefusjonsperiodeDto>? = null,
): ApiResult<Unit> =
    saksbehandlerVelgerInntekt(
        personPseudoId = personPseudoId,
        behandlingId = behandlingId,
        yrkesaktivitetId = yrkesaktivitetId,
        inntektskilde =
            InntektRequestDto.Arbeidstaker(
                data =
                    ArbeidstakerInntektRequestDto.Inntektsmelding(
                        inntektsmeldingId = inntektsmeldingId,
                        begrunnelse = begrunnelse,
                        refusjon = refusjonsperiodeDto,
                    ),
            ),
    )

suspend fun ApplicationTestBuilder.saksbehandlerSkjønnsfastsetterInntekt(
    personPseudoId: UUID,
    behandlingId: UUID,
    yrkesaktivitetId: UUID,
    årsinntekt: Double,
    årsak: ArbeidstakerSkjønnsfastsettelseÅrsakDto,
    begrunnelse: String,
    refusjon: List<RefusjonsperiodeDto>? = null,
): ApiResult<Unit> =
    saksbehandlerVelgerInntekt(
        personPseudoId = personPseudoId,
        behandlingId = behandlingId,
        yrkesaktivitetId = yrkesaktivitetId,
        inntektskilde =
            InntektRequestDto.Arbeidstaker(
                data =
                    ArbeidstakerInntektRequestDto.Skjønnsfastsatt(
                        årsinntekt = årsinntekt,
                        årsak = årsak,
                        begrunnelse = begrunnelse,
                        refusjon = refusjon,
                    ),
            ),
    )
