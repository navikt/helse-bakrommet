package no.nav.helse.bakrommet.saksbehandlingsperiode

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.runApplicationTest
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*

class DagoversiktLogikkTest {
    private companion object {
        const val FNR = "01019012349"
        const val PERSON_ID = "65hth"
    }

    @Test
    fun `oppretter dagoversikt automatisk når ER_SYKMELDT er ER_SYKMELDT_JA eller mangler`() {
        runApplicationTest { daoer ->
            daoer.personDao.opprettPerson(FNR, PERSON_ID)

            // Opprett en saksbehandlingsperiode først
            val periodeResponse =
                client.post("/v1/$PERSON_ID/saksbehandlingsperioder") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody("""{ "fom": "2023-01-01", "tom": "2023-01-31" }""")
                }
            assertEquals(201, periodeResponse.status.value)

            val allePerioder =
                client.get("/v1/$PERSON_ID/saksbehandlingsperioder") {
                    bearerAuth(TestOppsett.userToken)
                }
            val perioder: List<Saksbehandlingsperiode> = allePerioder.body()
            val periode = perioder.first()

            // Test 1: ER_SYKMELDT er "ER_SYKMELDT_JA" - skal opprette dagoversikt
            @Language("json")
            val kategoriseringMedSykmeldt =
                """
                {
                    "kategorisering": {
                        "INNTEKTSKATEGORI": "ARBEIDSTAKER",
                        "ER_SYKMELDT": "ER_SYKMELDT_JA",
                        "TYPE_ARBEIDSTAKER": "ORDINÆRT_ARBEIDSFORHOLD"
                    }
                }
                """.trimIndent()

            val inntektsforholdMedSykmeldtId =
                client.post("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/yrkesaktivitet") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(kategoriseringMedSykmeldt)
                }.let { response ->
                    assertEquals(201, response.status.value)
                    val body = response.body<JsonNode>()
                    UUID.fromString(body["id"].asText())
                }

            // Test 2: ER_SYKMELDT mangler - skal opprette dagoversikt
            @Language("json")
            val kategoriseringUtenSykmeldt =
                """
                {
                    "kategorisering": {
                        "INNTEKTSKATEGORI": "SELVSTENDIG_NÆRINGSDRIVENDE",
                        "TYPE_SELVSTENDIG_NÆRINGSDRIVENDE": "FISKER",
                        "SELVSTENDIG_NÆRINGSDRIVENDE_FORSIKRING": "FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG"
                    }
                }
                """.trimIndent()

            val inntektsforholdUtenSykmeldtId =
                client.post("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/yrkesaktivitet") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(kategoriseringUtenSykmeldt)
                }.let { response ->
                    assertEquals(201, response.status.value)
                    val body = response.body<JsonNode>()
                    UUID.fromString(body["id"].asText())
                }

            // Test 3: ER_SYKMELDT er "ER_SYKMELDT_NEI" - skal IKKE opprette dagoversikt
            @Language("json")
            val kategoriseringIkkeSykmeldt =
                """
                {
                    "kategorisering": {
                        "INNTEKTSKATEGORI": "ARBEIDSTAKER",
                        "ER_SYKMELDT": "ER_SYKMELDT_NEI",
                        "TYPE_ARBEIDSTAKER": "ORDINÆRT_ARBEIDSFORHOLD"
                    }
                }
                """.trimIndent()

            val inntektsforholdIkkeSykmeldtId =
                client.post("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/yrkesaktivitet") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(kategoriseringIkkeSykmeldt)
                }.let { response ->
                    assertEquals(201, response.status.value)
                    val body = response.body<JsonNode>()
                    UUID.fromString(body["id"].asText())
                }

            // Verifiser at dagoversikt ble opprettet for de to første, men ikke den siste
            daoer.yrkesaktivitetDao.hentYrkesaktivitetFor(periode).also { inntektsforholdFraDB ->
                val medSykmeldt = inntektsforholdFraDB.find { it.id == inntektsforholdMedSykmeldtId }!!
                val utenSykmeldt = inntektsforholdFraDB.find { it.id == inntektsforholdUtenSykmeldtId }!!
                val ikkeSykmeldt = inntektsforholdFraDB.find { it.id == inntektsforholdIkkeSykmeldtId }!!

                // Disse skal ha dagoversikt (31 dager for januar 2023)
                assertEquals(31, medSykmeldt.dagoversikt?.size() ?: 0, "Yrkesaktivitet med ER_SYKMELDT_JA skal ha dagoversikt")
                assertEquals(31, utenSykmeldt.dagoversikt?.size() ?: 0, "Yrkesaktivitet uten ER_SYKMELDT skal ha dagoversikt")

                // Denne skal IKKE ha dagoversikt
                assertTrue(
                    ikkeSykmeldt.dagoversikt == null || ikkeSykmeldt.dagoversikt?.size() == 0,
                    "Yrkesaktivitet med ER_SYKMELDT_NEI skal ikke ha dagoversikt",
                )
            }
        }
    }

    @Test
    fun `dagoversikt inneholder riktig antall dager for perioden`() {
        runApplicationTest { daoer ->
            daoer.personDao.opprettPerson(FNR, PERSON_ID)

            // Test med februar (kortere måned)
            client.post("/v1/$PERSON_ID/saksbehandlingsperioder") {
                bearerAuth(TestOppsett.userToken)
                contentType(ContentType.Application.Json)
                setBody("""{ "fom": "2023-02-01", "tom": "2023-02-28" }""")
            }

            val periode =
                client.get("/v1/$PERSON_ID/saksbehandlingsperioder") {
                    bearerAuth(TestOppsett.userToken)
                }.body<List<Saksbehandlingsperiode>>().first()

            val yrkesaktivitetId =
                client.post("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/yrkesaktivitet") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody("""{"kategorisering": {"INNTEKTSKATEGORI": "ARBEIDSTAKER", "ER_SYKMELDT": "ER_SYKMELDT_JA"}}""")
                }.let { response ->
                    UUID.fromString(response.body<JsonNode>()["id"].asText())
                }

            // Verifiser at dagoversikt har 28 dager for februar 2023
            daoer.yrkesaktivitetDao.hentYrkesaktivitetFor(periode).also { inntektsforholdFraDB ->
                val inntektsforhold = inntektsforholdFraDB.find { it.id == yrkesaktivitetId }!!
                assertEquals(28, inntektsforhold.dagoversikt?.size() ?: 0, "Februar 2023 skal ha 28 dager")
            }
        }
    }
}
