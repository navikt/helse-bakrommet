package no.nav.helse.bakrommet.sigrun

import kotlinx.coroutines.runBlocking
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.auth.SpilleromBearerToken
import no.nav.helse.bakrommet.util.Kildespor
import no.nav.helse.bakrommet.util.asJsonNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SigrunClientTest {
    companion object {
        fun client2010to2050(fnr: String) = SigrunMock.sigrunMockClient(fnrÅrTilSvar = fnrÅrTilSvar2010to2050(fnr))

        fun fnrÅrTilSvar2010to2050(fnr: String) =
            mapOf(
                *(2010..2100).map { år ->
                    (fnr to år.toString()) to sigrunÅr(fnr, år, næring = år * 100)
                }.toTypedArray(),
            )

        fun clientMedManglendeÅr(
            fnr: String,
            vararg manglendeÅr: Int,
        ): SigrunClient {
            val manglendeÅrStr: List<String> = manglendeÅr.map { it.toString() }

            val dataSomManglerNoenÅr =
                fnrÅrTilSvar2010to2050(fnr).mapValues { (fnrÅr, data) ->
                    if (fnrÅr.second in manglendeÅrStr) {
                        sigrunErrorResponse(status = 404, kode = "PGIF-008")
                    } else {
                        data
                    }
                }
            return SigrunMock.sigrunMockClient(fnrÅrTilSvar = dataSomManglerNoenÅr)
        }
    }

    val token = SpilleromBearerToken(TestOppsett.userToken)
    val fnr = "01017099999"

    private fun clientMedManglendeÅr(vararg manglendeÅr: Int) =
        Companion.clientMedManglendeÅr(
            fnr = fnr,
            manglendeÅr = manglendeÅr,
        )

    private fun List<PensjonsgivendeInntektÅrMedSporing>.tommeOgEksisterndeÅr(): Pair<Set<Int>, Set<Int>> {
        val tomme = mutableSetOf<Int>()
        val eksisterende = mutableSetOf<Int>()
        this.forEach {
            val år = it.data().inntektsaar()
            if (it.data()["pensjonsgivendeInntekt"].isNull) {
                tomme += år
            } else {
                assertEquals(1, it.data()["pensjonsgivendeInntekt"]!!.size())
                eksisterende += år
            }
        }
        return tomme to eksisterende
    }

    @Test
    fun `Hent 2024 og 3 tilbake, men 2024 mangler, gir 2023, 2022, 2021`() {
        val client = clientMedManglendeÅr(2024)
        runBlocking {
            client
                .hentPensjonsgivendeInntektForÅrSenestOgAntallÅrBakover(fnr, 2024, 3, token)
        }.also { listeMedSporing ->
            val årTilResMap = listeMedSporing.associate { it.data().inntektsaar() to it }

            val tom2024 =
                """{
                "norskPersonidentifikator":"$fnr","inntektsaar":"2024",
                "pensjonsgivendeInntekt": null
            }""".asJsonNode()
            val resultat2024 = årTilResMap[2024]!!
            assertEquals(tom2024, resultat2024.data())
            assertTrue(
                resultat2024.sporing().kilde.contains(
                    """
                    "ske-message":{"kode":"PGIF-008","melding":"Fant ikke pensjonsgivende inntekt for oppgitt personidentifikator og inntektsår."
                    """.trimIndent(),
                ),
                "manglende inntekt skal ha med responsen fra skatt i sporingen",
            )

            årTilResMap.filter { it.key != 2024 }.also { deAndre ->
                assertEquals(setOf(2021, 2022, 2023), deAndre.keys)
                deAndre.values.forEach { resp ->
                    assertEquals(
                        1,
                        resp.data()["pensjonsgivendeInntekt"]!!.size(),
                        "forventer at pensjonsgivendeInntekt er en array med ett objekt (og ikke null)",
                    )
                }
            }
        }
    }

    @Test
    fun `diverse caser med manglende år`() {
        runBlocking {
            clientMedManglendeÅr(2024)
                .hentPensjonsgivendeInntektForÅrSenestOgAntallÅrBakover(fnr, 2024, 3, token)
        }.tommeOgEksisterndeÅr().also { (tomme, eksisterende) ->
            assertEquals(setOf(2024), tomme)
            assertEquals(setOf(2021, 2022, 2023), eksisterende)
        }

        runBlocking {
            clientMedManglendeÅr(2023, 2024, 2025)
                .hentPensjonsgivendeInntektForÅrSenestOgAntallÅrBakover(fnr, 2025, 3, token)
        }.tommeOgEksisterndeÅr().also { (tomme, eksisterende) ->
            assertEquals(setOf(2023, 2024, 2025), tomme)
            assertEquals(setOf(2020, 2021, 2022), eksisterende)
        }

        runBlocking {
            clientMedManglendeÅr(2022, 2023, 2024, 2025)
                .hentPensjonsgivendeInntektForÅrSenestOgAntallÅrBakover(fnr, 2025, 3, token)
        }.tommeOgEksisterndeÅr().also { (tomme, eksisterende) ->
            assertEquals(setOf(2022, 2023, 2024, 2025), tomme)
            assertEquals(emptySet<Int>(), eksisterende)
        }

        runBlocking {
            clientMedManglendeÅr(2022, 2024, 2025)
                .hentPensjonsgivendeInntektForÅrSenestOgAntallÅrBakover(fnr, 2025, 3, token)
        }.tommeOgEksisterndeÅr().also { (tomme, eksisterende) ->
            assertEquals(setOf(2022, 2024, 2025), tomme)
            assertEquals(setOf(2021, 2023), eksisterende)
        }

        assertThrows<IllegalStateException>("tillater ikke mer enn 10") {
            runBlocking {
                clientMedManglendeÅr()
                    .hentPensjonsgivendeInntektForÅrSenestOgAntallÅrBakover(fnr, 2025, 11, token)
            }
        }

        runBlocking {
            clientMedManglendeÅr()
                .hentPensjonsgivendeInntektForÅrSenestOgAntallÅrBakover(fnr, 2025, 10, token)
        }.tommeOgEksisterndeÅr().also { (tomme, eksisterende) ->
            assertEquals(emptySet<Int>(), tomme)
            assertEquals((2017..2025).toList().toSet(), eksisterende, "uansett tidligst 2017")
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
