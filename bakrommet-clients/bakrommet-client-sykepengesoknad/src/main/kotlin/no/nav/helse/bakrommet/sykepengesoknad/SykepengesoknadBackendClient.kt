package no.nav.helse.bakrommet.sykepengesoknad

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson.JacksonConverter
import no.nav.helse.bakrommet.Kildespor
import no.nav.helse.bakrommet.auth.AccessToken
import no.nav.helse.bakrommet.auth.TokenUtvekslingProvider
import no.nav.helse.bakrommet.errorhandling.SoknadIkkeFunnetException
import no.nav.helse.bakrommet.infrastruktur.provider.SykepengesøknadProvider
import no.nav.helse.bakrommet.logg
import no.nav.helse.bakrommet.objectMapper
import no.nav.helse.bakrommet.serialisertTilString
import no.nav.helse.bakrommet.util.fraHer
import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
import java.time.LocalDate

class SykepengesoknadBackendClient(
    private val configuration: SykepengesøknadBackendClientModule.Configuration,
    private val tokenUtvekslingProvider: TokenUtvekslingProvider,
    private val httpClient: HttpClient =
        HttpClient(Apache) {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(objectMapper))
            }
        },
) : SykepengesøknadProvider {
    companion object {
        data class HentSoknaderRequest(
            val fnr: String,
            val medSporsmal: Boolean = false,
            val fom: LocalDate,
            val tom: LocalDate,
        )
    }

    private fun hentSoknaderRequest(
        fnr: String,
        fom: LocalDate,
        medSporsmal: Boolean,
    ): String =
        HentSoknaderRequest(
            fnr = fnr,
            fom = fom,
            tom = LocalDate.now().plusDays(100),
            medSporsmal = medSporsmal,
        ).serialisertTilString()

    override suspend fun hentSoknader(
        saksbehandlerToken: AccessToken,
        fnr: String,
        fom: LocalDate,
        medSporsmal: Boolean,
    ): List<SykepengesoknadDTO> {
        val response =
            httpClient.post("${configuration.hostname}/api/v3/soknader") {
                headers[HttpHeaders.Authorization] = "Bearer " + tokenUtvekslingProvider.exchangeToken(saksbehandlerToken, configuration.scope).value
                contentType(ContentType.Application.Json)
                setBody(hentSoknaderRequest(fnr = fnr, fom = fom, medSporsmal = medSporsmal))
            }
        if (response.status == HttpStatusCode.OK) {
            return response.body<List<SykepengesoknadDTO>>()
        } else {
            logg.warn("hentSoknader statusCode={}", response.status.value)
        }
        throw RuntimeException(
            "Feil ved henting av sykepengesoknader, status=${response.status.value}}",
        )
    }

    override suspend fun hentSoknad(
        saksbehandlerToken: AccessToken,
        id: String,
    ): SykepengesoknadDTO = hentSoknadMedSporing(saksbehandlerToken, id).first

    override suspend fun hentSoknadMedSporing(
        saksbehandlerToken: AccessToken,
        id: String,
    ): Pair<SykepengesoknadDTO, Kildespor> {
        val url = "${configuration.hostname}/api/v3/soknader/$id"
        val kildespor = configuration.appConfig.fraHer(Throwable(), id, url) // Inkluder saksbehandlerident?
        val response =
            httpClient.get(url) {
                headers[HttpHeaders.Authorization] = "Bearer " + tokenUtvekslingProvider.exchangeToken(saksbehandlerToken, configuration.scope).value
                contentType(ContentType.Application.Json)
            }
        if (response.status == HttpStatusCode.OK) {
            return response.body<SykepengesoknadDTO>() to kildespor
        }
        if (response.status == HttpStatusCode.NotFound) {
            throw SoknadIkkeFunnetException("Søknad med id=$id ble ikke funnet")
        }
        throw RuntimeException(
            "Feil ved henting av sykepengesoknad, status=${response.status.value}}",
        )
    }
}
