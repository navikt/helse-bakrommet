import io.ktor.http.HttpStatusCode
import no.nav.helse.bakrommet.e2e.testutils.ApiResult

fun <T> ApiResult<T>.forventOk(): T {
    check(this is ApiResult.Success) { "Forventet ApiResult.Success" }
    check(status == HttpStatusCode.OK) { "Forventet HttpStatusCode.OK, fikk $status" }
    return this.response
}

fun <T> ApiResult<T>.forventCreated(): T {
    check(this is ApiResult.Success) { "Forventet ApiResult.Success" }
    check(status == HttpStatusCode.Created) { "Forventet HttpStatusCode.Created, fikk $status" }
    return this.response
}

fun <T> ApiResult<T>.forventNoContent(): T {
    check(this is ApiResult.Success) { "Forventet ApiResult.Success" }
    check(status == HttpStatusCode.NoContent) { "Forventet HttpStatusCode.NoContent, fikk $status" }
    return this.response
}
