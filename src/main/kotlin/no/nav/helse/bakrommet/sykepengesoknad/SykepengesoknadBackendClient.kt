package no.nav.helse.bakrommet.sykepengesoknad

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import no.nav.helse.bakrommet.Configuration
import no.nav.helse.bakrommet.auth.OboClient
import no.nav.helse.bakrommet.auth.SpilleromBearerToken
import no.nav.helse.bakrommet.errorhandling.SoknadIkkeFunnetException
import no.nav.helse.bakrommet.util.Kildespor
import no.nav.helse.bakrommet.util.logg
import no.nav.helse.bakrommet.util.objectMapper
import no.nav.helse.bakrommet.util.serialisertTilString
import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
import java.time.LocalDate

class SykepengesoknadBackendClient(
    private val configuration: Configuration.SykepengesoknadBackend,
    private val oboClient: OboClient,
    private val httpClient: HttpClient =
        HttpClient(Apache) {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(objectMapper))
            }
        },
) {
    private fun hentSoknaderRequest(
        fnr: String,
        fom: LocalDate,
    ): String {
        data class HentSoknaderRequest(
            val fnr: String,
            val medSporsmal: Boolean = false,
            val fom: LocalDate,
            val tom: LocalDate,
        )

        return HentSoknaderRequest(
            fnr = fnr,
            fom = fom,
            tom = LocalDate.now().plusDays(100),
            medSporsmal = false,
        ).serialisertTilString()
    }

    private suspend fun SpilleromBearerToken.tilOboBearerHeader(): String =
        this.exchangeWithObo(oboClient, configuration.scope).somBearerHeader()

    suspend fun hentSoknader(
        saksbehandlerToken: SpilleromBearerToken,
        fnr: String,
        fom: LocalDate,
    ): List<SykepengesoknadDTO> {
        val response =
            httpClient.post("${configuration.hostname}/api/v3/soknader") {
                headers[HttpHeaders.Authorization] = saksbehandlerToken.tilOboBearerHeader()
                contentType(ContentType.Application.Json)
                setBody(hentSoknaderRequest(fnr = fnr, fom = fom))
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

    suspend fun hentSoknad(
        saksbehandlerToken: SpilleromBearerToken,
        id: String,
    ): SykepengesoknadDTO {
        return hentSoknadMedSporing(saksbehandlerToken, id).first
    }

    suspend fun hentSoknadMedSporing(
        saksbehandlerToken: SpilleromBearerToken,
        id: String,
    ): Pair<SykepengesoknadDTO, Kildespor> {
        val url = "${configuration.hostname}/api/v3/soknader/$id"
        val kildespor = Kildespor.fraHer(Throwable(), id, url) // Inkluder saksbehandlerident?
        val response =
            httpClient.get(url) {
                headers[HttpHeaders.Authorization] = saksbehandlerToken.tilOboBearerHeader()
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
