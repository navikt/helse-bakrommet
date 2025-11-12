package no.nav.helse.bakrommet.ereg

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.fakerConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import no.nav.helse.bakrommet.Configuration
import no.nav.helse.bakrommet.util.objectMapper
import org.slf4j.LoggerFactory
import java.util.Random

object EregMock {
    private val log = LoggerFactory.getLogger(EregMock::class.java)

    // Default test konfigurasjon
    val defaultConfiguration =
        Configuration.Ereg(
            baseUrl = "https://ereg-services.test",
        )

    fun eregMockHttpClient() =
        mockHttpClient { request ->
            log.info("URL: " + request.url)
            log.info("BODY: " + String(request.body.toByteArray()))
            log.info("HEADERS: " + request.headers)

            // Hent orgnummer fra URL: /v2/organisasjon/{orgnummer}/noekkelinfo
            val urlPath = request.url.encodedPath
            val orgnummerMatch = Regex("/v2/organisasjon/([^/]+)/noekkelinfo").find(urlPath)
            val orgnummer = orgnummerMatch?.groupValues?.get(1)

            if (orgnummer == null) {
                respond(
                    status = HttpStatusCode.BadRequest,
                    content = "Ugyldig URL",
                )
            } else {
                val organisasjon =
                    organisasjonsnavnMap[orgnummer]
                        ?: if (!orgnummer.startsWith("1") && orgnummer.length == 9) {
                            // Generer navn med faker for orgnumre som ikke begynner på 1 og har 9 siffer
                            val seed = orgnummer.hashCode().toLong()
                            val config =
                                fakerConfig {
                                    locale = "nb_NO"
                                    random = Random(seed)
                                }
                            val faker = Faker(config)
                            val generertNavn = faker.company.name()
                            Organisasjon(navn = generertNavn, orgnummer = orgnummer)
                        } else {
                            null
                        }

                if (organisasjon == null) {
                    respond(
                        status = HttpStatusCode.NotFound,
                        content = "Organisasjon ikke funnet",
                    )
                } else {
                    val svar = """{"navn": {"sammensattnavn": "${organisasjon.navn}"}}"""
                    respond(
                        status = HttpStatusCode.OK,
                        content = svar,
                        headers = headersOf("Content-Type" to listOf("application/json")),
                    )
                }
            }
        }

    fun eregClientMock(
        configuration: Configuration.Ereg = defaultConfiguration,
    ) = EregClient(
        configuration = configuration,
        httpClient = eregMockHttpClient(),
    )
}

// Helper functions for mock HTTP client
fun mockHttpClient(requestHandler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
    HttpClient(MockEngine) {
        install(ContentNegotiation) {
            register(ContentType.Application.Json, JacksonConverter(objectMapper))
        }
        engine {
            addHandler(requestHandler)
        }
    }

suspend fun HttpRequestData.bodyToJson(): JsonNode = jacksonObjectMapper().readValue(body.toByteArray(), JsonNode::class.java)
