package no.nav.helse.bakrommet.ereg

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
import no.nav.helse.bakrommet.clients.EregProvider
import no.nav.helse.bakrommet.clients.Organisasjon
import no.nav.helse.bakrommet.errorhandling.IkkeFunnetException
import no.nav.helse.bakrommet.util.logg
import no.nav.helse.bakrommet.util.sikkerLogger

data class EregNoekkelinfoResponse(
    val navn: EregNavn?,
)

data class EregNavn(
    val sammensattnavn: String?,
    val navnelinje1: String?,
)

class EregClient(
    private val configuration: Configuration.Ereg,
    private val httpClient: HttpClient =
        HttpClient(Apache) {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter())
            }
        },
) : EregProvider {
    override suspend fun hentOrganisasjonsnavn(
        orgnummer: String,
    ): Organisasjon {
        val url = "${configuration.baseUrl}/v2/organisasjon/$orgnummer/noekkelinfo"

        val response =
            httpClient.get(url) {
                header(HttpHeaders.Accept, ContentType.Application.Json)
            }

        if (response.status == HttpStatusCode.OK) {
            val data = response.body<JsonNode>()
            val navnNode = data.get("navn")
            if (navnNode != null) {
                val sammensattnavn = navnNode.get("sammensattnavn")?.asText()
                val navnelinje1 = navnNode.get("navnelinje1")?.asText()
                val navnFraEreg = sammensattnavn ?: navnelinje1
                if (navnFraEreg != null) {
                    return Organisasjon(navn = navnFraEreg, orgnummer = orgnummer)
                }
            }
            logg.warn("Mangler navn i EREG respons for orgnummer={}", orgnummer)
            sikkerLogger.warn("Mangler navn i EREG respons for orgnummer={} body={}", orgnummer, response.bodyAsText())
            throw IkkeFunnetException(
                title = "Organisasjon ikke funnet",
                detail = "Fant ikke navn for organisasjonsnummer $orgnummer",
            )
        }

        if (response.status == HttpStatusCode.NotFound) {
            logg.warn("Fant ikke organisasjon i EREG for orgnummer={}", orgnummer)
            throw IkkeFunnetException(
                title = "Organisasjon ikke funnet",
                detail = "Fant ikke organisasjon i EREG for organisasjonsnummer $orgnummer",
            )
        }

        logg.error("EREG kall feilet med status={} for orgnummer={}", response.status.value, orgnummer)
        sikkerLogger.error("EREG kall feilet med status={} for orgnummer={} body={}", response.status.value, orgnummer, response.bodyAsText())
        throw RuntimeException("Klarte ikke slå opp organisasjon i EREG (status ${response.status.value})")
    }
}
