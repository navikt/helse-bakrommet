package no.nav.helse.bakrommet.aareg

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
import no.nav.helse.bakrommet.auth.OboClient
import no.nav.helse.bakrommet.auth.SpilleromBearerToken
import no.nav.helse.bakrommet.clients.AARegProvider
import no.nav.helse.bakrommet.errorhandling.ForbiddenException
import no.nav.helse.bakrommet.util.Kildespor
import no.nav.helse.bakrommet.util.logg
import no.nav.helse.bakrommet.util.sikkerLogger
import java.util.*

typealias Arbeidsforholdoppslag = JsonNode

class AARegClient(
    private val configuration: Configuration.AAReg,
    private val oboClient: OboClient,
    private val httpClient: HttpClient =
        HttpClient(Apache) {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter())
            }
        },
) : AARegProvider {
    private suspend fun SpilleromBearerToken.tilOboBearerHeader(): String = this.exchangeWithObo(oboClient, configuration.scope).somBearerHeader()

    override suspend fun hentArbeidsforholdFor(
        fnr: String,
        saksbehandlerToken: SpilleromBearerToken,
    ): Arbeidsforholdoppslag =
        hentArbeidsforholdForMedSporing(
            fnr = fnr,
            saksbehandlerToken = saksbehandlerToken,
        ).first

    override suspend fun hentArbeidsforholdForMedSporing(
        fnr: String,
        saksbehandlerToken: SpilleromBearerToken,
    ): Pair<Arbeidsforholdoppslag, Kildespor> {
        val callId: String = UUID.randomUUID().toString()
        val arbeidsforholdstatusFilter = "AKTIV,AVSLUTTET,FREMTIDIG"
        val url = "https://${configuration.hostname}/api/v2/arbeidstaker/arbeidsforhold"

        val kildespor =
            Kildespor.fraHer(
                Throwable(),
                fnr,
                arbeidsforholdstatusFilter,
                url,
                callId,
            ) // Inkluder saksbehandlerident?

        val response =
            httpClient.get(url) {
                headers[HttpHeaders.Authorization] = saksbehandlerToken.tilOboBearerHeader()
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header("Nav-Call-Id", callId)
                header("Nav-Personident", fnr)
                parameter("arbeidsforholdstatus", arbeidsforholdstatusFilter) // Default i V2 er: AKTIV,FREMTIDIG
                // parameter("historikk", "true") // TODO
                // parameter("regelverk", "ALLE") // TODO
            }
        if (response.status == HttpStatusCode.OK) {
            return response.body<JsonNode>() to kildespor
        } else {
            logg.warn("hentArbeidsforholdFor statusCode={} callId={}", response.status.value, callId)
            sikkerLogger.warn("hentArbeidsforholdFor statusCode={} callId={} body={}", response.status.value, callId, response.bodyAsText())
        }
        if (response.status == HttpStatusCode.Forbidden) {
            throw ForbiddenException("Ikke tilstrekkelig tilgang i AA-REG")
        }
        throw RuntimeException(
            "Feil ved henting av arbeidsforhold, status=${response.status.value}, callId=$callId",
        )
    }
}
