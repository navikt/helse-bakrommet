package no.nav.helse.bakrommet.sykepengesoknad

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import no.nav.helse.bakrommet.Configuration
import no.nav.helse.bakrommet.auth.OboToken
import no.nav.helse.bakrommet.util.logg
import no.nav.helse.bakrommet.util.serialisertTilString
import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
import java.time.LocalDate

class SykepengesoknadBackendClient(
    private val configuration: Configuration.SykepengesoknadBackend,
    private val httpClient: HttpClient =
        HttpClient(Apache) {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter())
            }
        },
) {
    private fun hentSoknaderRequest(fnr: String): String {
        data class HentSoknaderRequest(
            val fnr: String,
            val medSporsmal: Boolean = false,
            val fom: LocalDate,
            val tom: LocalDate,
        )

        return HentSoknaderRequest(
            fnr = fnr,
            fom = LocalDate.now().minusDays(200),
            tom = LocalDate.now().plusDays(100),
            medSporsmal = false,
        ).serialisertTilString()
    }

    suspend fun hentSoknader(
        sykepengesoknadToken: OboToken,
        fnr: String,
    ): List<SykepengesoknadDTO> {
        val response =
            httpClient.post("${configuration.hostname}/api/v3/soknader") {
                headers[HttpHeaders.Authorization] = sykepengesoknadToken.somBearerHeader()
                contentType(ContentType.Application.Json)
                setBody(hentSoknaderRequest(fnr = fnr))
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
}
