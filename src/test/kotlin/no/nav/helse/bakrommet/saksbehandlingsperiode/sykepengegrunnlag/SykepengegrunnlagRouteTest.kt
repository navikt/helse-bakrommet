package no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.helse.bakrommet.Daoer
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.runApplicationTest
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold.Inntektsforhold
import no.nav.helse.bakrommet.testutils.truncateTidspunkt
import no.nav.helse.bakrommet.util.asJsonNode
import no.nav.helse.bakrommet.util.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.*

class SykepengegrunnlagRouteTest {
    private companion object {
        val fnr = "01019012349"
        val personId = "65hth"
    }

    fun sykepengegrunnlagAppTest(
        testBlock: suspend ApplicationTestBuilder.(Triple<Daoer, Saksbehandlingsperiode, Inntektsforhold>) -> Unit,
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
        val inntektsforhold =
            Inntektsforhold(
                id = UUID.randomUUID(),
                kategorisering = """{"INNTEKTSKATEGORI": "ARBEIDSTAKER", "orgnummer": "123456789"}""".asJsonNode(),
                kategoriseringGenerert = null,
                dagoversikt = """[]""".asJsonNode(),
                dagoversiktGenerert = null,
                saksbehandlingsperiodeId = saksbehandlingsperiode.id,
                opprettet = OffsetDateTime.now(),
                generertFraDokumenter = emptyList(),
            )
        val lagretInntektsforhold = it.inntektsforholdDao.opprettInntektsforhold(inntektsforhold)

        this.testBlock(Triple(it, saksbehandlingsperiode, lagretInntektsforhold))
    }

    @Test
    fun `oppretter sykepengegrunnlag med faktiske inntekter`() =
        sykepengegrunnlagAppTest { (daoer, saksbehandlingsperiode, inntektsforhold) ->
            val requestBody =
                """
                {
                    "faktiskeInntekter": [
                        {
                            "inntektsforholdId": "${inntektsforhold.id}",
                            "beløpPerMånedØre": 4500000,
                            "kilde": "SAKSBEHANDLER",
                            "erSkjønnsfastsatt": false
                        }
                    ],
                    "begrunnelse": "Standard saksbehandling"
                }
                """.trimIndent()

            val response =
                client.post("/v1/$personId/saksbehandlingsperioder/${saksbehandlingsperiode.id}/sykepengegrunnlag") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }

            assertEquals(HttpStatusCode.Created, response.status)

            val responseBody = objectMapper.readTree(response.bodyAsText())
            assertEquals(54000000L, responseBody["totalInntektØre"].asLong()) // 4.5M * 12
            assertEquals(74416800L, responseBody["grunnbeløp6GØre"].asLong()) // 6G
            assertEquals(false, responseBody["begrensetTil6G"].asBoolean())
            assertEquals(54000000L, responseBody["sykepengegrunnlagØre"].asLong())
            assertEquals("Standard saksbehandling", responseBody["begrunnelse"].asText())
            assertEquals(1, responseBody["versjon"].asInt())
        }

    @Test
    fun `beregner sykepengegrunnlag med 6G-begrensning`() =
        sykepengegrunnlagAppTest { (daoer, saksbehandlingsperiode, inntektsforhold) ->
            val requestBody =
                """
                {
                    "faktiskeInntekter": [
                        {
                            "inntektsforholdId": "${inntektsforhold.id}",
                            "beløpPerMånedØre": 8000000,
                            "kilde": "SAKSBEHANDLER",
                            "erSkjønnsfastsatt": false
                        }
                    ]
                }
                """.trimIndent()

            val response =
                client.post("/v1/$personId/saksbehandlingsperioder/${saksbehandlingsperiode.id}/sykepengegrunnlag") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }

            assertEquals(HttpStatusCode.Created, response.status)

            val responseBody = objectMapper.readTree(response.bodyAsText())
            assertEquals(96000000L, responseBody["totalInntektØre"].asLong()) // 8M * 12
            assertEquals(74416800L, responseBody["grunnbeløp6GØre"].asLong()) // 6G
            assertEquals(true, responseBody["begrensetTil6G"].asBoolean())
            assertEquals(74416800L, responseBody["sykepengegrunnlagØre"].asLong()) // Begrenset til 6G
        }

    @Test
    fun `oppretter sykepengegrunnlag med skjønnsfastsettelse og refusjon`() =
        sykepengegrunnlagAppTest { (daoer, saksbehandlingsperiode, inntektsforhold) ->
            val requestBody =
                """
                {
                    "faktiskeInntekter": [
                        {
                            "inntektsforholdId": "${inntektsforhold.id}",
                            "beløpPerMånedØre": 5000000,
                            "kilde": "SKJONNSFASTSETTELSE",
                            "erSkjønnsfastsatt": true,
                            "skjønnsfastsettelseBegrunnelse": "Inntekt fastsatt skjønnsmessig grunnet manglende dokumentasjon",
                            "refusjon": {
                                "refusjonsbeløpPerMånedØre": 5000000,
                                "refusjonsgrad": 100
                            }
                        }
                    ],
                    "begrunnelse": "Skjønnsfastsettelse med full refusjon"
                }
                """.trimIndent()

            val response =
                client.post("/v1/$personId/saksbehandlingsperioder/${saksbehandlingsperiode.id}/sykepengegrunnlag") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }

            assertEquals(HttpStatusCode.Created, response.status)

            val responseBody = objectMapper.readTree(response.bodyAsText())
            val faktiskeInntekter = responseBody["faktiskeInntekter"]
            assertEquals(1, faktiskeInntekter.size())

            val faktiskInntekt = faktiskeInntekter[0]
            assertEquals(true, faktiskInntekt["erSkjønnsfastsatt"].asBoolean())
            assertEquals(
                "Inntekt fastsatt skjønnsmessig grunnet manglende dokumentasjon",
                faktiskInntekt["skjønnsfastsettelseBegrunnelse"].asText(),
            )

            val refusjon = faktiskInntekt["refusjon"]
            assertEquals(5000000L, refusjon["refusjonsbeløpPerMånedØre"].asLong())
            assertEquals(100, refusjon["refusjonsgrad"].asInt())
        }

    @Test
    fun `henter eksisterende sykepengegrunnlag`() =
        sykepengegrunnlagAppTest { (daoer, saksbehandlingsperiode, inntektsforhold) ->
            // Opprett først sykepengegrunnlag
            val requestBody =
                """
                {
                    "faktiskeInntekter": [
                        {
                            "inntektsforholdId": "${inntektsforhold.id}",
                            "beløpPerMånedØre": 4500000,
                            "kilde": "SAKSBEHANDLER",
                            "erSkjønnsfastsatt": false
                        }
                    ]
                }
                """.trimIndent()

            client.post("/v1/$personId/saksbehandlingsperioder/${saksbehandlingsperiode.id}/sykepengegrunnlag") {
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
            assertEquals(1, responseBody["faktiskeInntekter"].size())
        }

    @Test
    fun `oppdaterer eksisterende sykepengegrunnlag`() =
        sykepengegrunnlagAppTest { (daoer, saksbehandlingsperiode, inntektsforhold) ->
            // Opprett først sykepengegrunnlag
            val opprettRequestBody =
                """
                {
                    "faktiskeInntekter": [
                        {
                            "inntektsforholdId": "${inntektsforhold.id}",
                            "beløpPerMånedØre": 4000000,
                            "kilde": "SAKSBEHANDLER",
                            "erSkjønnsfastsatt": false
                        }
                    ]
                }
                """.trimIndent()

            client.post("/v1/$personId/saksbehandlingsperioder/${saksbehandlingsperiode.id}/sykepengegrunnlag") {
                bearerAuth(TestOppsett.userToken)
                contentType(ContentType.Application.Json)
                setBody(opprettRequestBody)
            }

            // Oppdater sykepengegrunnlag
            val oppdaterRequestBody =
                """
                {
                    "faktiskeInntekter": [
                        {
                            "inntektsforholdId": "${inntektsforhold.id}",
                            "beløpPerMånedØre": 5500000,
                            "kilde": "SKJONNSFASTSETTELSE",
                            "erSkjønnsfastsatt": true,
                            "skjønnsfastsettelseBegrunnelse": "Justert etter gjennomgang"
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
            assertEquals(2, responseBody["versjon"].asInt()) // Versjon økt
        }

    @Test
    fun `sletter sykepengegrunnlag`() =
        sykepengegrunnlagAppTest { (daoer, saksbehandlingsperiode, inntektsforhold) ->
            // Opprett først sykepengegrunnlag
            val requestBody =
                """
                {
                    "faktiskeInntekter": [
                        {
                            "inntektsforholdId": "${inntektsforhold.id}",
                            "beløpPerMånedØre": 4500000,
                            "kilde": "SAKSBEHANDLER",
                            "erSkjønnsfastsatt": false
                        }
                    ]
                }
                """.trimIndent()

            client.post("/v1/$personId/saksbehandlingsperioder/${saksbehandlingsperiode.id}/sykepengegrunnlag") {
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

            assertEquals(HttpStatusCode.NotFound, getResponse.status)
        }

    @Test
    fun `får 404 ved henting av ikke-eksisterende sykepengegrunnlag`() =
        sykepengegrunnlagAppTest { (daoer, saksbehandlingsperiode, inntektsforhold) ->
            val getResponse =
                client.get("/v1/$personId/saksbehandlingsperioder/${saksbehandlingsperiode.id}/sykepengegrunnlag") {
                    bearerAuth(TestOppsett.userToken)
                }

            assertEquals(HttpStatusCode.NotFound, getResponse.status)
        }

    @Test
    fun `får 400 ved ugyldige data`() =
        sykepengegrunnlagAppTest { (daoer, saksbehandlingsperiode, inntektsforhold) ->
            val ugyldigRequestBody =
                """
                {
                    "faktiskeInntekter": [
                        {
                            "inntektsforholdId": "${inntektsforhold.id}",
                            "beløpPerMånedØre": -1000,
                            "kilde": "UGYLDIG_KILDE"
                        }
                    ]
                }
                """.trimIndent()

            val response =
                client.post("/v1/$personId/saksbehandlingsperioder/${saksbehandlingsperiode.id}/sykepengegrunnlag") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(ugyldigRequestBody)
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
}
