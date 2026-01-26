package no.nav.helse.bakrommet.e2e.testutils

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import no.nav.helse.bakrommet.errorhandling.ProblemDetails

sealed class ApiResult<out T> {
    data class Success<out T>(
        val response: T,
    ) : ApiResult<T>()

    data class Error(
        val problemDetails: ProblemDetails,
    ) : ApiResult<Nothing>()
}

internal suspend inline fun <reified T> HttpResponse.result(onSuccess: T.() -> Unit) =
    if (this.status.isSuccess()) {
        val body = this.body<T>()
        onSuccess(body)
        ApiResult.Success(body)
    } else {
        ApiResult.Error(body<ProblemDetails>())
    }
