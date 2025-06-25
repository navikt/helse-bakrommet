package no.nav.helse.bakrommet.sigrun

import kotlinx.coroutines.runBlocking
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.auth.SpilleromBearerToken
import no.nav.helse.bakrommet.util.Kildespor
import no.nav.helse.bakrommet.util.asJsonNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SigrunClientTest {
    val token = SpilleromBearerToken(TestOppsett.userToken)

    val fnr = "01017099999"

    val fnrÅrTilSvar2010to2050 =
        mapOf(
            *(2010..2100).map { år ->
                (fnr to år.toString()) to sigrunÅr(fnr, år, næring = år * 100)
            }.toTypedArray(),
        )

    val client2010to2050 = SigrunMock.sigrunMockClient(fnrÅrTilSvar = fnrÅrTilSvar2010to2050)

    @Test
    fun `manglende pensjonsgivende inntekt for et år skal mappe til en respons med pensjonsgivendeInntekt=null`() {
        val dataSomMangler2020 =
            fnrÅrTilSvar2010to2050.mapValues {
                if (it.key == (fnr to "2020")) {
                    sigrunErrorResponse(status = 404, kode = "PGIF-008")
                } else {
                    it.value
                }
            }
        val client = SigrunMock.sigrunMockClient(fnrÅrTilSvar = dataSomMangler2020)
        runBlocking {
            client
                .hentPensjonsgivendeInntektForPeriodeMedSporing(fnr, 2019, 2022, token)
        }.also { listeMedSporing ->
            val årTilResMap = listeMedSporing.associate { it.data().inntektsaar() to it }

            val tom2020 =
                """{
                "norskPersonidentifikator":"$fnr","inntektsaar":"2020",
                "pensjonsgivendeInntekt": null
            }""".asJsonNode()
            val resultat2020 = årTilResMap[2020]!!
            assertEquals(tom2020, resultat2020.data())
            assertTrue(
                resultat2020.sporing().kilde.contains(
                    """
                    "ske-message":{"kode":"PGIF-008","melding":"Fant ikke pensjonsgivende inntekt for oppgitt personidentifikator og inntektsår."
                    """.trimIndent(),
                ),
                "manglende inntekt skal ha med responsen fra skatt i sporingen",
            )

            årTilResMap.filter { it.key != 2020 }.also { deAndre ->
                assertEquals(3, deAndre.size)
                assertEquals(setOf(2019, 2021, 2022), deAndre.keys)
                deAndre.values.forEach { resp ->
                    assertEquals(
                        1,
                        resp.data()["pensjonsgivendeInntekt"]!!.size(),
                        "forventer at pensjonsgivendeInntekt er en array med ett objekt (og ikke null)",
                    )
                }
            }
            runBlocking {
                client
                    .hentPensjonsgivendeInntektForPeriode(fnr, 2019, 2022, token)
            }.also { listeUtenSporing ->
                assertEquals(
                    listeMedSporing.map { it.data() }.sortedBy { it.inntektsaar() },
                    listeUtenSporing.sortedBy { it.inntektsaar() },
                )
            }
        }
    }

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

    @Test
    fun `Henter år fom 2020, tom 2024`() {
        runBlocking {
            client2010to2050
                .hentPensjonsgivendeInntektForPeriode(fnr, 2020, 2024, token)
        }.also { liste ->
            assertEquals(setOf(2020, 2021, 2022, 2023, 2024), liste.map { it.inntektsaar() }.toSet())
        }
    }
}

fun Pair<PensjonsgivendeInntektÅr, Kildespor>.data() = this.first

fun Pair<PensjonsgivendeInntektÅr, Kildespor>.sporing() = this.second

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

private fun sigrunErrorResponse(
    status: Int = 404,
    kode: String = "PGIF-008",
) = """
    {"timestamp":"2025-06-24T09:36:23.209+0200","status":$status,"error":"Not Found","source":"SKE",
    "message":"Fant ikke pensjonsgivende inntekt for oppgitt personidentifikator og inntektsår..  Korrelasjonsid: bb918c1c1cfa10a396e723949ae25f80. Spurt på år 2021 og tjeneste Pensjonsgivende Inntekt For Folketrygden",
    "path":"/api/v1/pensjonsgivendeinntektforfolketrygden",
    "ske-message":{"kode":"$kode","melding":"Fant ikke pensjonsgivende inntekt for oppgitt personidentifikator og inntektsår.","korrelasjonsid":"bb918c1c1cfa10a396e723949ae25f80"}}    
    """.trimIndent()
