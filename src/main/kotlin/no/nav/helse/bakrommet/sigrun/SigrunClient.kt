package no.nav.helse.bakrommet.sigrun

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import no.nav.helse.bakrommet.Configuration
import no.nav.helse.bakrommet.ainntekt.Inntektoppslag
import no.nav.helse.bakrommet.auth.OboClient
import no.nav.helse.bakrommet.auth.SpilleromBearerToken
import no.nav.helse.bakrommet.errorhandling.ForbiddenException
import no.nav.helse.bakrommet.util.Kildespor
import no.nav.helse.bakrommet.util.logg
import no.nav.helse.bakrommet.util.sikkerLogger
import java.util.*

class SigrunClient(
    private val configuration: Configuration.Sigrun,
    private val oboClient: OboClient,
    private val httpClient: HttpClient =
        HttpClient(Apache) {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter())
            }
        },
) {
    private suspend fun SpilleromBearerToken.tilOboBearerHeader(): String =
        this.exchangeWithObo(oboClient, configuration.scope).somBearerHeader()

    suspend fun hentPensjonsgivendeInntekt(
        fnr: String,
        inntektsAar: Int,
        saksbehandlerToken: SpilleromBearerToken,
    ): Inntektoppslag {
        return hentPensjonsgivendeInntektMedSporing(
            fnr = fnr,
            inntektsAar = inntektsAar,
            saksbehandlerToken = saksbehandlerToken,
        ).first
    }

    suspend fun hentPensjonsgivendeInntektMedSporing(
        fnr: String,
        inntektsAar: Int,
        saksbehandlerToken: SpilleromBearerToken,
    ): Pair<Inntektoppslag, Kildespor> {
        val callId: String = UUID.randomUUID().toString()
        val callIdDesc = " callId=$callId"
        val rettighetspakke = "navSykepenger"
        val url = "${configuration.baseUrl}/api/v1/pensjonsgivendeinntektforfolketrygden"
        val kildespor =
            Kildespor.fraHer(
                Throwable(),
                fnr,
                inntektsAar,
                rettighetspakke,
                url,
                callId,
            ) // Inkluder saksbehandlerident?
        val timer = Timer()
        val response =
            httpClient.get(url) {
                headers[HttpHeaders.Authorization] = saksbehandlerToken.tilOboBearerHeader()
                header("Nav-Consumer-Id", "bakrommet-speilvendt")
                header("Nav-Call-Id", callId)

                header("rettighetspakke", rettighetspakke)
                header("Nav-Personident", fnr)
                header("inntektsaar", inntektsAar.toString())

                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
            }
        if (response.status == HttpStatusCode.OK) {
            logg.info("Got response from sigrun $callIdDesc etter ${timer.millisekunder} ms")
            return response.body<JsonNode>() to kildespor
        } else {
            logg.error(
                "Feil under henting av PensjonsgivendeInntekt etter ${timer.millisekunder} ms: ${response.status}, Se secureLog for detaljer $callIdDesc",
            )
            sikkerLogger.error("Feil under henting av PensjonsgivendeInntekt: ${response.status} - ${response.bodyAsText()} $callIdDesc")
            if (response.status == HttpStatusCode.Forbidden) {
                throw ForbiddenException("Ikke tilstrekkelig tilgang i Sigrun")
            }
            throw RuntimeException(
                "Feil ved henting av PensjonsgivendeInntekt, status=${response.status.value}, callId=$callId",
            )
        }
    }
}

private class Timer {
    val startMS: Long = System.currentTimeMillis()
    val millisekunder: Long
        get() = System.currentTimeMillis() - startMS
}
