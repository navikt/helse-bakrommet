package no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.helse.bakrommet.Daoer
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.runApplicationTest
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Yrkesaktivitet
import no.nav.helse.bakrommet.testutils.truncateTidspunkt
import no.nav.helse.bakrommet.util.asJsonNode
import no.nav.helse.bakrommet.util.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.*

class SykepengegrunnlagRouteTest {
    private companion object {
        val fnr = "01019012349"
        val personId = "65hth"
    }

    fun sykepengegrunnlagAppTest(
        testBlock: suspend ApplicationTestBuilder.(Triple<Daoer, Saksbehandlingsperiode, Yrkesaktivitet>) -> Unit,
    ) = runApplicationTest {
        it.personDao.opprettPerson(fnr, personId)

        // Opprett saksbehandlingsperiode
        val saksperiodeResponse =
            client.post("/v1/$personId/saksbehandlingsperioder") {
                bearerAuth(TestOppsett.userToken)
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    { "fom": "2023-01-01", "tom": "2023-01-31" }
                    """.trimIndent(),
                )
            }
        assertEquals(201, saksperiodeResponse.status.value)

        val saksbehandlingsperiode: Saksbehandlingsperiode =
            objectMapper.readValue(
                saksperiodeResponse.bodyAsText(),
                Saksbehandlingsperiode::class.java,
            ).truncateTidspunkt()

        // Opprett inntektsforhold
        val yrkesaktivitet =
            Yrkesaktivitet(
                id = UUID.randomUUID(),
                kategorisering = """{"INNTEKTSKATEGORI": "ARBEIDSTAKER", "orgnummer": "123456789"}""".asJsonNode(),
                kategoriseringGenerert = null,
                dagoversikt = emptyList(),
                dagoversiktGenerert = null,
                saksbehandlingsperiodeId = saksbehandlingsperiode.id,
                opprettet = OffsetDateTime.now(),
                generertFraDokumenter = emptyList(),
            )
        val lagretYrkesaktivitet = it.yrkesaktivitetDao.opprettYrkesaktivitet(yrkesaktivitet)

        this.testBlock(Triple(it, saksbehandlingsperiode, lagretYrkesaktivitet))
    }

    @Test
    fun `lagrer sykepengegrunnlag med inntekter`() =
        sykepengegrunnlagAppTest { (daoer, saksbehandlingsperiode, inntektsforhold) ->
            val requestBody =
                """
                {
                    "inntekter": [
                        {
                            "yrkesaktivitetId": "${inntektsforhold.id}",
                            "beløpPerMånedØre": 4500000,
                            "kilde": "AINNTEKT"
                        }
                    ],
                    "begrunnelse": "Standard saksbehandling"
                }
                """.trimIndent()

            val response =
                client.put("/v1/$personId/saksbehandlingsperioder/${saksbehandlingsperiode.id}/sykepengegrunnlag") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }

            assertEquals(HttpStatusCode.OK, response.status)

            val responseBody = objectMapper.readTree(response.bodyAsText())
            assertEquals(54000000L, responseBody["totalInntektØre"].asLong()) // 4.5M * 12
            assertTrue(
                responseBody["grunnbeløpØre"].asLong() == 12402800L || responseBody["grunnbeløpØre"].asLong() == 13016000L || responseBody["grunnbeløpØre"].asLong() == 11147700L,
            ) // 1G
            assertTrue(
                responseBody["grunnbeløp6GØre"].asLong() == 74416800L || responseBody["grunnbeløp6GØre"].asLong() == 78096000L || responseBody["grunnbeløp6GØre"].asLong() == 66886200L,
            ) // 6G
            assertEquals(false, responseBody["begrensetTil6G"].asBoolean())
            assertEquals(54000000L, responseBody["sykepengegrunnlagØre"].asLong())
            assertEquals("Standard saksbehandling", responseBody["begrunnelse"].asText())
        }

    @Test
    fun `beregner sykepengegrunnlag med 6G-begrensning`() =
        sykepengegrunnlagAppTest { (daoer, saksbehandlingsperiode, inntektsforhold) ->
            val requestBody =
                """
                {
                    "inntekter": [
                        {
                            "yrkesaktivitetId": "${inntektsforhold.id}",
                            "beløpPerMånedØre": 8000000,
                            "kilde": "AINNTEKT"
                        }
                    ]
                }
                """.trimIndent()

            val response =
                client.put("/v1/$personId/saksbehandlingsperioder/${saksbehandlingsperiode.id}/sykepengegrunnlag") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }

            assertEquals(HttpStatusCode.OK, response.status)

            val responseBody = objectMapper.readTree(response.bodyAsText())
            assertEquals(96000000L, responseBody["totalInntektØre"].asLong()) // 8M * 12
            assertTrue(
                responseBody["grunnbeløpØre"].asLong() == 12402800L || responseBody["grunnbeløpØre"].asLong() == 13016000L || responseBody["grunnbeløpØre"].asLong() == 11147700L,
            ) // 1G
            assertTrue(
                responseBody["grunnbeløp6GØre"].asLong() == 74416800L || responseBody["grunnbeløp6GØre"].asLong() == 78096000L || responseBody["grunnbeløp6GØre"].asLong() == 66886200L,
            ) // 6G
            assertEquals(true, responseBody["begrensetTil6G"].asBoolean())
            assertEquals(responseBody["grunnbeløp6GØre"].asLong(), responseBody["sykepengegrunnlagØre"].asLong()) // Begrenset til 6G
        }

    @Test
    fun `lagrer sykepengegrunnlag med skjønnsfastsettelse og refusjon`() =
        sykepengegrunnlagAppTest { (daoer, saksbehandlingsperiode, inntektsforhold) ->
            val requestBody =
                """
                {
                    "inntekter": [
                        {
                            "yrkesaktivitetId": "${inntektsforhold.id}",
                            "beløpPerMånedØre": 5000000,
                            "kilde": "SKJONNSFASTSETTELSE",
                            "refusjon": [
                                {
                                    "fom": "2023-01-01",
                                    "tom": "2023-01-31",
                                    "beløpØre": 5000000
                                }
                            ]
                        }
                    ],
                    "begrunnelse": "Skjønnsfastsettelse med full refusjon"
                }
                """.trimIndent()

            val response =
                client.put("/v1/$personId/saksbehandlingsperioder/${saksbehandlingsperiode.id}/sykepengegrunnlag") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }

            assertEquals(HttpStatusCode.OK, response.status)

            val responseBody = objectMapper.readTree(response.bodyAsText())
            val inntekter = responseBody["inntekter"]
            assertEquals(1, inntekter.size())

            val inntekt = inntekter[0]
            assertEquals("SKJONNSFASTSETTELSE", inntekt["kilde"].asText())

            val refusjon = inntekt["refusjon"]
            assertEquals(1, refusjon.size())

            val refusjonsperiode = refusjon[0]
            assertEquals("2023-01-01", refusjonsperiode["fom"].asText())
            assertEquals("2023-01-31", refusjonsperiode["tom"].asText())
            assertEquals(5000000L, refusjonsperiode["beløpØre"].asLong())
        }

    @Test
    fun `henter eksisterende sykepengegrunnlag`() =
        sykepengegrunnlagAppTest { (daoer, saksbehandlingsperiode, inntektsforhold) ->
            // Opprett først sykepengegrunnlag
            val requestBody =
                """
                {
                    "inntekter": [
                        {
                            "yrkesaktivitetId": "${inntektsforhold.id}",
                            "beløpPerMånedØre": 4500000,
                            "kilde": "AINNTEKT"
                        }
                    ]
                }
                """.trimIndent()

            client.put("/v1/$personId/saksbehandlingsperioder/${saksbehandlingsperiode.id}/sykepengegrunnlag") {
                bearerAuth(TestOppsett.userToken)
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            // Hent sykepengegrunnlag
            val getResponse =
                client.get("/v1/$personId/saksbehandlingsperioder/${saksbehandlingsperiode.id}/sykepengegrunnlag") {
                    bearerAuth(TestOppsett.userToken)
                }

            assertEquals(HttpStatusCode.OK, getResponse.status)

            val responseBody = objectMapper.readTree(getResponse.bodyAsText())
            assertEquals(54000000L, responseBody["totalInntektØre"].asLong())
            assertEquals(1, responseBody["inntekter"].size())
        }

    @Test
    fun `oppdaterer eksisterende sykepengegrunnlag`() =
        sykepengegrunnlagAppTest { (daoer, saksbehandlingsperiode, inntektsforhold) ->
            // Opprett først sykepengegrunnlag
            val opprettRequestBody =
                """
                {
                    "inntekter": [
                        {
                            "yrkesaktivitetId": "${inntektsforhold.id}",
                            "beløpPerMånedØre": 4000000,
                            "kilde": "AINNTEKT"
                        }
                    ]
                }
                """.trimIndent()

            client.put("/v1/$personId/saksbehandlingsperioder/${saksbehandlingsperiode.id}/sykepengegrunnlag") {
                bearerAuth(TestOppsett.userToken)
                contentType(ContentType.Application.Json)
                setBody(opprettRequestBody)
            }

            // Oppdater sykepengegrunnlag
            val oppdaterRequestBody =
                """
                {
                    "inntekter": [
                        {
                            "yrkesaktivitetId": "${inntektsforhold.id}",
                            "beløpPerMånedØre": 5500000,
                            "kilde": "SKJONNSFASTSETTELSE"
                        }
                    ],
                    "begrunnelse": "Oppdatert etter ny vurdering"
                }
                """.trimIndent()

            val putResponse =
                client.put("/v1/$personId/saksbehandlingsperioder/${saksbehandlingsperiode.id}/sykepengegrunnlag") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(oppdaterRequestBody)
                }

            assertEquals(HttpStatusCode.OK, putResponse.status)

            val responseBody = objectMapper.readTree(putResponse.bodyAsText())
            assertEquals(66000000L, responseBody["totalInntektØre"].asLong()) // 5.5M * 12
            assertEquals("Oppdatert etter ny vurdering", responseBody["begrunnelse"].asText())
        }

    @Test
    fun `sletter sykepengegrunnlag`() =
        sykepengegrunnlagAppTest { (daoer, saksbehandlingsperiode, inntektsforhold) ->
            // Opprett først sykepengegrunnlag
            val requestBody =
                """
                {
                    "inntekter": [
                        {
                            "yrkesaktivitetId": "${inntektsforhold.id}",
                            "beløpPerMånedØre": 4500000,
                            "kilde": "AINNTEKT"
                        }
                    ]
                }
                """.trimIndent()

            client.put("/v1/$personId/saksbehandlingsperioder/${saksbehandlingsperiode.id}/sykepengegrunnlag") {
                bearerAuth(TestOppsett.userToken)
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            // Slett sykepengegrunnlag
            val deleteResponse =
                client.delete("/v1/$personId/saksbehandlingsperioder/${saksbehandlingsperiode.id}/sykepengegrunnlag") {
                    bearerAuth(TestOppsett.userToken)
                }

            assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

            // Verifiser at det er slettet
            val getResponse =
                client.get("/v1/$personId/saksbehandlingsperioder/${saksbehandlingsperiode.id}/sykepengegrunnlag") {
                    bearerAuth(TestOppsett.userToken)
                }

            assertEquals(HttpStatusCode.OK, getResponse.status)
            assertEquals("null", getResponse.bodyAsText())
        }

    @Test
    fun `får null ved henting av ikke-eksisterende sykepengegrunnlag`() =
        sykepengegrunnlagAppTest { (daoer, saksbehandlingsperiode, inntektsforhold) ->
            val getResponse =
                client.get("/v1/$personId/saksbehandlingsperioder/${saksbehandlingsperiode.id}/sykepengegrunnlag") {
                    bearerAuth(TestOppsett.userToken)
                }

            assertEquals(HttpStatusCode.OK, getResponse.status)
            assertEquals("null", getResponse.bodyAsText())
        }

    @Test
    fun `får 400 ved ugyldige data`() =
        sykepengegrunnlagAppTest { (daoer, saksbehandlingsperiode, inntektsforhold) ->
            val ugyldigRequestBody =
                """
                {
                    "inntekter": [
                        {
                            "yrkesaktivitetId": "${inntektsforhold.id}",
                            "beløpPerMånedØre": -1000,
                            "kilde": "UGYLDIG_KILDE"
                        }
                    ]
                }
                """.trimIndent()

            val response =
                client.put("/v1/$personId/saksbehandlingsperioder/${saksbehandlingsperiode.id}/sykepengegrunnlag") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(ugyldigRequestBody)
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
}
