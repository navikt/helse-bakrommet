package no.nav.helse.bakrommet.ainntekt

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import no.nav.helse.bakrommet.Configuration
import no.nav.helse.bakrommet.auth.OboClient
import no.nav.helse.bakrommet.auth.SpilleromBearerToken
import no.nav.helse.bakrommet.errorhandling.ForbiddenException
import no.nav.helse.bakrommet.infrastruktur.provider.AInntektFilter
import no.nav.helse.bakrommet.infrastruktur.provider.InntekterProvider
import no.nav.helse.bakrommet.infrastruktur.provider.Inntektoppslag
import no.nav.helse.bakrommet.util.Kildespor
import no.nav.helse.bakrommet.util.logg
import no.nav.helse.bakrommet.util.sikkerLogger
import java.time.YearMonth
import java.util.*

class AInntektClient(
    private val configuration: Configuration.AInntekt,
    private val oboClient: OboClient,
    private val httpClient: HttpClient =
        HttpClient(Apache) {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter())
            }
            engine {
                connectionRequestTimeout = 20_000
                socketTimeout = 20_000 // default 10_000
            }
        },
) : InntekterProvider {
    private suspend fun SpilleromBearerToken.tilOboBearerHeader(): String = this.exchangeWithObo(oboClient, configuration.scope).somBearerHeader()

    override suspend fun hentInntekterForMedSporing(
        fnr: String,
        maanedFom: YearMonth,
        maanedTom: YearMonth,
        filter: AInntektFilter,
        saksbehandlerToken: SpilleromBearerToken,
    ): Pair<Inntektoppslag, Kildespor> {
        val ainntektsfilter = filter.name
        val callId: String = UUID.randomUUID().toString()
        val callIdDesc = " callId=$callId"
        val url = "https://${configuration.hostname}/rest/v2/inntekt"
        val kildespor =
            Kildespor.fraHer(
                Throwable(),
                fnr,
                maanedFom,
                maanedTom,
                ainntektsfilter,
                url,
                callId,
            ) // Inkluder saksbehandlerident?
        val timer = Timer()
        val response =
            httpClient.post(url) {
                headers[HttpHeaders.Authorization] = saksbehandlerToken.tilOboBearerHeader()
                header("Nav-Consumer-Id", "bakrommet-speilvendt")
                header("Nav-Call-Id", callId)
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("personident", fnr)
                        put("filter", ainntektsfilter)
                        put("formaal", "Sykepenger")
                        put("maanedFom", maanedFom.toString())
                        put("maanedTom", maanedTom.toString())
                    }.toString(),
                )
            }
        if (response.status == HttpStatusCode.OK) {
            logg.info("Got response from inntektskomponenten $callIdDesc etter ${timer.millisekunder} ms")
            return response.body<JsonNode>() to kildespor
        } else {
            logg.error(
                "Feil under henting av inntekter etter ${timer.millisekunder} ms: ${response.status}, Se secureLog for detaljer $callIdDesc",
            )
            sikkerLogger.error("Feil under henting av inntekter: ${response.status} - ${response.bodyAsText()} $callIdDesc")
            if (response.status == HttpStatusCode.Forbidden) {
                throw ForbiddenException("Ikke tilstrekkelig tilgang i A-Inntekt")
            }
            throw RuntimeException(
                "Feil ved henting av inntekter, status=${response.status.value}, callId=$callId",
            )
        }
    }
}

private class Timer {
    val startMS: Long = System.currentTimeMillis()
    val millisekunder: Long
        get() = System.currentTimeMillis() - startMS
}
