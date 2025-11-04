package no.nav.helse.bakrommet.ereg

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import no.nav.helse.bakrommet.Configuration
import no.nav.helse.bakrommet.util.objectMapper
import org.slf4j.LoggerFactory

object EregMock {
    private val log = LoggerFactory.getLogger(EregMock::class.java)

    // Testdata fra organisasjonsnavnMap i spillerom
    val organisasjonsnavnMap: Map<String, String> =
        mapOf(
            "987654321" to "Kranførerkompaniet",
            "123456789" to "Krankompisen",
            "889955555" to "Danskebåten",
            "972674818" to "Pengeløs Sparebank",
            "222222222" to "Ruter, avd Nesoddbåten",
            "805824352" to "Vegansk slakteri",
            "896929119" to "Sauefabrikk",
            "947064649" to "Sjokkerende elektriker",
            "967170232" to "Snill torpedo",
            "839942907" to "Hårreisende frisør",
            "907670201" to "Klonelabben",
            "999999991" to "Murstein AS",
            "999999992" to "Betongbygg AS",
            "963743254" to "Veihjelpen AS",
        )

    // Default test konfigurasjon
    val defaultConfiguration =
        Configuration.Ereg(
            baseUrl = "https://ereg-services.test",
        )

    fun eregMockHttpClient(
        configuration: Configuration.Ereg = defaultConfiguration,
        orgnummerTilNavn: Map<String, String> = organisasjonsnavnMap,
    ) = mockHttpClient { request ->
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
            val navn = orgnummerTilNavn[orgnummer]
            if (navn == null) {
                respond(
                    status = HttpStatusCode.NotFound,
                    content = "Organisasjon ikke funnet",
                )
            } else {
                val svar = """{"navn": {"sammensattnavn": "$navn"}}"""
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
        orgnummerTilNavn: Map<String, String> = organisasjonsnavnMap,
    ) = EregClient(
        configuration = configuration,
        httpClient = eregMockHttpClient(configuration, orgnummerTilNavn),
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
