package no.nav.helse.bakrommet.ereg

import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.fakerConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import no.nav.helse.bakrommet.client.common.ApplicationConfig
import no.nav.helse.bakrommet.client.common.mockHttpClient
import no.nav.helse.bakrommet.infrastruktur.provider.Organisasjon
import org.slf4j.LoggerFactory
import java.util.*

object EregMock {
    private val log = LoggerFactory.getLogger(EregMock::class.java)

    // Default test konfigurasjon
    val defaultConfiguration =
        EregClientModule.Configuration(
            baseUrl = "https://ereg-services.test",
            appConfig =
                ApplicationConfig(
                    podName = "unknownHost",
                    appName = "unknownApp",
                    imageName = "unknownImage",
                ),
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
        configuration: EregClientModule.Configuration = defaultConfiguration,
    ) = EregClient(
        configuration = configuration,
        httpClient = eregMockHttpClient(),
    )
}
