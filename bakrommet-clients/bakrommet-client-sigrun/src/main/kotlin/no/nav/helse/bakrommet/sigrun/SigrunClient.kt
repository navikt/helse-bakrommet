package no.nav.helse.bakrommet.sigrun

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson.JacksonConverter
import no.nav.helse.bakrommet.auth.SpilleromBearerToken
import no.nav.helse.bakrommet.auth.TokenUtvekslingProvider
import no.nav.helse.bakrommet.errorhandling.ForbiddenException
import no.nav.helse.bakrommet.infrastruktur.provider.PensjonsgivendeInntektProvider
import no.nav.helse.bakrommet.infrastruktur.provider.PensjonsgivendeInntektÅr
import no.nav.helse.bakrommet.infrastruktur.provider.PensjonsgivendeInntektÅrMedSporing
import no.nav.helse.bakrommet.infrastruktur.provider.data
import no.nav.helse.bakrommet.util.Kildespor
import no.nav.helse.bakrommet.util.asJsonNode
import no.nav.helse.bakrommet.util.logg
import no.nav.helse.bakrommet.util.sikkerLogger
import java.lang.Integer.max
import java.time.LocalDate
import java.util.*

class SigrunClient(
    private val configuration: SigrunClientModule.Configuration,
    private val tokenUtvekslingProvider: TokenUtvekslingProvider,
    private val httpClient: HttpClient =
        HttpClient(Apache) {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter())
            }
        },
) : PensjonsgivendeInntektProvider {
    private suspend fun SpilleromBearerToken.tilOboBearerHeader(): String = this.exchangeWithObo(tokenUtvekslingProvider, configuration.scope).somBearerHeader()

    companion object {
        val INNTEKTSAAR_MIN = 2017
        val INNTEKTSAAR_MAX: Int
            get() = LocalDate.now().year
        val INNTEKTSAAR_MAX_COUNT = 10
    }

    override suspend fun hentPensjonsgivendeInntektForÅrSenestOgAntallÅrBakover(
        fnr: String,
        senesteÅrTom: Int,
        antallÅrBakover: Int,
        saksbehandlerToken: SpilleromBearerToken,
    ): List<`PensjonsgivendeInntektÅrMedSporing`> {
        check(antallÅrBakover <= INNTEKTSAAR_MAX_COUNT)
        check(senesteÅrTom <= INNTEKTSAAR_MAX)

        val absoluttEldsteMulige = max(INNTEKTSAAR_MIN, senesteÅrTom - (2 * antallÅrBakover))

        val hentedeÅr = mutableListOf<`PensjonsgivendeInntektÅrMedSporing`>()
        for (år in senesteÅrTom downTo absoluttEldsteMulige) {
            val res =
                hentPensjonsgivendeInntektMedSporing(
                    fnr = fnr,
                    inntektsAar = år,
                    saksbehandlerToken = saksbehandlerToken,
                )
            hentedeÅr += res

            val antallTommeIStarten = hentedeÅr.takeWhile { !it.data().harFastsattPensjonsgivendeInntekt() }.size
            val antallÅrTilOgMedSisteFastsatte = hentedeÅr.size - antallTommeIStarten

            if (antallTommeIStarten > antallÅrBakover) {
                break // Gir opp (max 4 tomme hvis 3 etc.. TODO: Revurderer dette ?)
            }
            if (antallÅrTilOgMedSisteFastsatte >= antallÅrBakover) {
                break
            }
        }
        return hentedeÅr
    }

    private suspend fun hentPensjonsgivendeInntektMedSporing(
        fnr: String,
        inntektsAar: Int,
        saksbehandlerToken: SpilleromBearerToken,
    ): Pair<`PensjonsgivendeInntektÅr`, Kildespor> {
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
            return response.body<`PensjonsgivendeInntektÅr`>() to kildespor
        } else {
            if (response.status == HttpStatusCode.NotFound) {
                if (response.betyrIngenPensjonsgivendeInntektFunnet()) {
                    return tomResponsFor(fnr = fnr, år = inntektsAar) to
                        kildespor.medTillegg(
                            mapOf("reponse" to response.bodyAsText()),
                        )
                }
            }
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

private fun `PensjonsgivendeInntektÅr`.pensjonsgivendeInntekt(): JsonNode? = this["pensjonsgivendeInntekt"]

private fun `PensjonsgivendeInntektÅr`.harFastsattPensjonsgivendeInntekt(): Boolean =
    this.pensjonsgivendeInntekt().let {
        (it != null) && (!it.isNull)
    }

private fun tomResponsFor(
    fnr: String,
    år: Int,
): `PensjonsgivendeInntektÅr` =
    """
    {"norskPersonidentifikator":"$fnr","inntektsaar":"$år",
    "pensjonsgivendeInntekt": null
    }        
    """.trimIndent().asJsonNode()

/**
 * Avgjør om repons betyr Ingen Pensjonsgivende Inntekt Funnet...
 * Ref: https://skatteetaten.github.io/api-dokumentasjon/api/pgi_folketrygden?tab=Feilkoder
 */
private suspend fun HttpResponse.betyrIngenPensjonsgivendeInntektFunnet() = (this.status == HttpStatusCode.NotFound) && (this.bodyAsText().contains("PGIF-008"))

private class Timer {
    val startMS: Long = System.currentTimeMillis()
    val millisekunder: Long
        get() = System.currentTimeMillis() - startMS
}
