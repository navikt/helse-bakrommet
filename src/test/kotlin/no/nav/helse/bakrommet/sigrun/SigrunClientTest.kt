package no.nav.helse.bakrommet.sigrun

import kotlinx.coroutines.runBlocking
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.auth.SpilleromBearerToken
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class SigrunClientTest {
    val token = SpilleromBearerToken(TestOppsett.userToken)

    val fnr = "01017099999"

    val client2010to2050 =
        SigrunMock.sigrunMockClient(
            fnrÅrTilSvar =
                mapOf(
                    *(2010..2100).map { år ->
                        (fnr to år.toString()) to sigrunÅr(fnr, år, næring = år * 100)
                    }.toTypedArray(),
                ),
        )

    @Test
    fun `Henter ikke år tidligere enn 2017 eller senere enn inneværende år`() {
        // NB: Vil kunne feile på antall > SigrunClient.INNTEKTSAAR_MAX_COUNT i 2027 hvis INNTEKTSAAR_MAX_COUNT=10
        runBlocking {
            client2010to2050
                .hentPensjonsgivendeInntektForPeriode(fnr, 2015, 2080, token)
        }.also { liste ->
            val alleÅrIRespons = liste.map { it.inntektsaar() }
            assertEquals(2017, alleÅrIRespons.min(), "henter ikke år tidligere enn 2017")
            assertEquals(LocalDate.now().year, alleÅrIRespons.max(), "henter ikke år senere enn inneværende år")
        }
    }
}

private fun PensjonsgivendeInntektÅr.inntektsaar() = this["inntektsaar"]!!.asText().toInt()

private fun sigrunÅr(
    fnr: String = "10419045026",
    år: Int = 2022,
    næring: Int = 350000,
) = """
    {"norskPersonidentifikator":"$fnr","inntektsaar":"$år",
    "pensjonsgivendeInntekt":
        [
            {"skatteordning":"FASTLAND","datoForFastsetting":"2025-06-24T07:32:48.777Z",
            "pensjonsgivendeInntektAvLoennsinntekt":null,
            "pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel":null,
            "pensjonsgivendeInntektAvNaeringsinntekt":"$næring",
            "pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage":"10000"}
        ]
    }    
    """.trimIndent()

private fun sigrunErrorResponse(kode: String = "PGIF-008") =
    """
    {"timestamp":"2025-06-24T09:36:23.209+0200","status":404,"error":"Not Found","source":"SKE",
    "message":"Fant ikke pensjonsgivende inntekt for oppgitt personidentifikator og inntektsår..  Korrelasjonsid: bb918c1c1cfa10a396e723949ae25f80. Spurt på år 2021 og tjeneste Pensjonsgivende Inntekt For Folketrygden",
    "path":"/api/v1/pensjonsgivendeinntektforfolketrygden",
    "ske-message":{"kode":"$kode-008","melding":"Fant ikke pensjonsgivende inntekt for oppgitt personidentifikator og inntektsår.","korrelasjonsid":"bb918c1c1cfa10a396e723949ae25f80"}}    
    """.trimIndent()
