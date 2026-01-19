package no.nav.helse.bakrommet.sigrun

import kotlinx.coroutines.runBlocking
import no.nav.helse.bakrommet.Kildespor
import no.nav.helse.bakrommet.asJsonNode
import no.nav.helse.bakrommet.auth.AccessToken
import no.nav.helse.bakrommet.infrastruktur.provider.PensjonsgivendeInntektÅr
import no.nav.helse.bakrommet.infrastruktur.provider.PensjonsgivendeInntektÅrMedSporing
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Year

class SigrunClientTest {
    val token = AccessToken("wsdfsdfsdf")
    val fnr = "01017099999"

    private fun clientMedManglendeÅrInt(vararg manglendeÅr: Int) =
        clientMedManglendeÅr(
            fnr = fnr,
            manglendeÅr = manglendeÅr.map { Year.of(it) }.toTypedArray(),
        )

    private fun clientMedManglendeÅr(vararg manglendeÅr: Year) =
        clientMedManglendeÅr(
            fnr = fnr,
            manglendeÅr = manglendeÅr,
        )

    private fun List<`PensjonsgivendeInntektÅrMedSporing`>.tommeOgEksisterndeÅr(): Pair<Set<Int>, Set<Int>> {
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
        val client = clientMedManglendeÅr(Year.of(2024))
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
            val kilde = resultat2024.sporing().kilde
            assertTrue(
                kilde.contains(
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
            clientMedManglendeÅr(Year.of(2024))
                .hentPensjonsgivendeInntektForÅrSenestOgAntallÅrBakover(fnr, 2024, 3, token)
        }.tommeOgEksisterndeÅr().also { (tomme, eksisterende) ->
            assertEquals(setOf(2024), tomme)
            assertEquals(setOf(2021, 2022, 2023), eksisterende)
        }

        runBlocking {
            clientMedManglendeÅrInt(2023, 2024, 2025)
                .hentPensjonsgivendeInntektForÅrSenestOgAntallÅrBakover(fnr, 2025, 3, token)
        }.tommeOgEksisterndeÅr().also { (tomme, eksisterende) ->
            assertEquals(setOf(2023, 2024, 2025), tomme)
            assertEquals(setOf(2020, 2021, 2022), eksisterende)
        }

        runBlocking {
            clientMedManglendeÅrInt(2022, 2023, 2024, 2025)
                .hentPensjonsgivendeInntektForÅrSenestOgAntallÅrBakover(fnr, 2025, 3, token)
        }.tommeOgEksisterndeÅr().also { (tomme, eksisterende) ->
            assertEquals(setOf(2022, 2023, 2024, 2025), tomme)
            assertEquals(emptySet<Int>(), eksisterende)
        }

        runBlocking {
            clientMedManglendeÅrInt(2022, 2024, 2025)
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

fun Pair<`PensjonsgivendeInntektÅr`, Kildespor>.data() = this.first

fun Pair<`PensjonsgivendeInntektÅr`, Kildespor>.sporing() = this.second

private fun `PensjonsgivendeInntektÅr`.inntektsaar() = this["inntektsaar"]!!.asText().toInt()
