package no.nav.helse.bakrommet.aareg

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.JacksonConverter
import no.nav.helse.bakrommet.auth.SpilleromBearerToken
import no.nav.helse.bakrommet.auth.TokenUtvekslingProvider
import no.nav.helse.bakrommet.errorhandling.ForbiddenException
import no.nav.helse.bakrommet.infrastruktur.provider.ArbeidsforholdProvider
import no.nav.helse.bakrommet.infrastruktur.provider.Arbeidsforholdoppslag
import no.nav.helse.bakrommet.util.Kildespor
import no.nav.helse.bakrommet.util.logg
import no.nav.helse.bakrommet.util.sikkerLogger
import java.util.*

class AARegClient(
    private val configuration: AAregModule.Configuration,
    private val tokenUtvekslingProvider: TokenUtvekslingProvider,
    private val httpClient: HttpClient =
        HttpClient(Apache) {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter())
            }
        },
) : ArbeidsforholdProvider {
    private suspend fun SpilleromBearerToken.tilOboBearerHeader(): String = this.exchangeWithObo(tokenUtvekslingProvider, configuration.scope).somBearerHeader()

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
