package no.nav.helse.bakrommet.saksbehandlingsperiode.vilkaar

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.helse.bakrommet.Daoer
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.runApplicationTest
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.testutils.truncateTidspunkt
import no.nav.helse.bakrommet.util.objectMapper
import no.nav.helse.bakrommet.util.somListe
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VilkårRouteTest {
    private companion object {
        val fnr = "01019012349"
        val personId = "65hth"
    }

    fun vilkårAppTest(testBlock: suspend ApplicationTestBuilder.(Pair<Daoer, Saksbehandlingsperiode>) -> Unit) =
        runApplicationTest {
            it.personDao.opprettPerson(fnr, personId)
            val response =
                client.post("/v1/$personId/saksbehandlingsperioder") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        { "fom": "2023-01-01", "tom": "2023-01-31" }
                        """.trimIndent(),
                    )
                }
            assertEquals(201, response.status.value)

            val saksbehandlingsperiode: Saksbehandlingsperiode =
                objectMapper
                    .readValue(
                        response.bodyAsText(),
                        Saksbehandlingsperiode::class.java,
                    ).truncateTidspunkt()

            this.testBlock(it to saksbehandlingsperiode)
        }

    @Test
    fun `oppretter et vurdert vilkår på saksbehandlingsperiode`() =
        vilkårAppTest { (daoer, saksbehandlingsperiode) ->
            saksbehandlingsperiode.id
            val vilkårPutResponse =
                client.put(
                    "/v1/$personId/saksbehandlingsperioder/${saksbehandlingsperiode.id}/vilkaarsvurdering/BOR_I_NORGE",
                ) {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {
                            "vurdering": "OPPFYLT",
                            "årsak": "derfor"
                        }
                        """.trimIndent(),
                    )
                }

            assertEquals(HttpStatusCode.Created, vilkårPutResponse.status)
            assertEquals(
                """{"vurdering":"OPPFYLT","årsak":"derfor","hovedspørsmål":"BOR_I_NORGE"}""",
                vilkårPutResponse.bodyAsText(),
            )
        }

    @Test
    fun `oppretter, endrer, legger til, henter og sletter vurdert vilkår på saksbehandlingsperiode`() =
        vilkårAppTest { (daoer, saksbehandlingsperiode) ->
            saksbehandlingsperiode.id
            client
                .put(
                    "/v1/$personId/saksbehandlingsperioder/${saksbehandlingsperiode.id}/vilkaarsvurdering/BOR_I_NORGE",
                ) {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {
                            
                                "vurdering": "OPPFYLT",
                                "årsak": "derfor"
                            
                        }
                        """.trimIndent(),
                    )
                }.apply {
                    assertEquals(HttpStatusCode.Created, status)
                    assertEquals(
                        """{"vurdering":"OPPFYLT","årsak":"derfor","hovedspørsmål":"BOR_I_NORGE"}""",
                        bodyAsText(),
                    )
                }

            client
                .get("/v1/$personId/saksbehandlingsperioder/${saksbehandlingsperiode.id}/vilkaarsvurdering") {
                    bearerAuth(TestOppsett.userToken)
                }.apply {
                    assertEquals(HttpStatusCode.OK, status)
                    assertEquals(
                        """[{"vurdering":"OPPFYLT","årsak":"derfor","hovedspørsmål":"BOR_I_NORGE"}]""",
                        bodyAsText(),
                    )
                }

            client
                .put(
                    "/v1/$personId/saksbehandlingsperioder/${saksbehandlingsperiode.id}/vilkaarsvurdering/BOR_I_NORGE",
                ) {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {
                          
                                "vurdering": "IKKE_OPPFYLT",
                                "årsak": "BOR_IKKE_I_NORGE"
                            
                        }
                        """.trimIndent(),
                    )
                }.apply {
                    assertEquals(HttpStatusCode.OK, status)
                    assertEquals(
                        """{"vurdering":"IKKE_OPPFYLT","årsak":"BOR_IKKE_I_NORGE","hovedspørsmål":"BOR_I_NORGE"}""",
                        bodyAsText(),
                    )
                }

            client
                .get("/v1/$personId/saksbehandlingsperioder/${saksbehandlingsperiode.id}/vilkaarsvurdering") {
                    bearerAuth(TestOppsett.userToken)
                }.apply {
                    assertEquals(HttpStatusCode.OK, status)
                    assertEquals(
                        """[{"vurdering":"IKKE_OPPFYLT","årsak":"BOR_IKKE_I_NORGE","hovedspørsmål":"BOR_I_NORGE"}]""",
                        bodyAsText(),
                    )
                }

            client
                .put(
                    "/v1/$personId/saksbehandlingsperioder/${saksbehandlingsperiode.id}/vilkaarsvurdering/ET_VILKÅR_TIL",
                ) {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {
                        
                                "vurdering": "IKKE_AKTUELT"
                            
                        }
                        """.trimIndent(),
                    )
                }.apply {
                    assertEquals(HttpStatusCode.Created, status)
                    assertEquals(
                        """{"vurdering":"IKKE_AKTUELT","hovedspørsmål":"ET_VILKÅR_TIL"}""",
                        bodyAsText(),
                    )
                }

            client
                .get("/v1/$personId/saksbehandlingsperioder/${saksbehandlingsperiode.id}/vilkaarsvurdering") {
                    bearerAuth(TestOppsett.userToken)
                }.apply {
                    assertEquals(HttpStatusCode.OK, status)
                    assertEquals(
                        setOf(
                            mapOf(
                                "vurdering" to "IKKE_OPPFYLT",
                                "årsak" to "BOR_IKKE_I_NORGE",
                                "hovedspørsmål" to "BOR_I_NORGE",
                            ),
                            mapOf(
                                "vurdering" to "IKKE_AKTUELT",
                                "hovedspørsmål" to "ET_VILKÅR_TIL",
                            ),
                        ),
                        bodyAsText().somListe<Map<String, String>>().toSet(),
                    )
                }

            client
                .delete(
                    "/v1/$personId/saksbehandlingsperioder/${saksbehandlingsperiode.id}/vilkaarsvurdering/BOR_I_NORGE",
                ) {
                    bearerAuth(TestOppsett.userToken)
                }.apply {
                    assertEquals(HttpStatusCode.NoContent, status, "skal gi 204 når koden fantes og er blitt slettet")
                }

            client
                .delete(
                    "/v1/$personId/saksbehandlingsperioder/${saksbehandlingsperiode.id}/vilkaarsvurdering/BOR_I_NORGE",
                ) {
                    bearerAuth(TestOppsett.userToken)
                }.apply {
                    assertEquals(HttpStatusCode.NotFound, status, "skal gi 404 når koden ikke fantes")
                }

            client
                .get("/v1/$personId/saksbehandlingsperioder/${saksbehandlingsperiode.id}/vilkaarsvurdering") {
                    bearerAuth(TestOppsett.userToken)
                }.apply {
                    assertEquals(HttpStatusCode.OK, status)
                    assertEquals(
                        setOf(
                            mapOf(
                                "hovedspørsmål" to "ET_VILKÅR_TIL",
                                "vurdering" to "IKKE_AKTUELT",
                            ),
                        ),
                        bodyAsText().somListe<Map<String, String>>().toSet(),
                    )
                }
        }

    @Test
    fun `ugyldig kode-format gir 400 med beskrivelse`() =
        vilkårAppTest { (daoer, saksbehandlingsperiode) ->
            saksbehandlingsperiode.id
            val vilkårPostResponse =
                client.put(
                    "/v1/$personId/saksbehandlingsperioder/${saksbehandlingsperiode.id}/vilkaarsvurdering/ugyldig-KODE",
                ) {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {
                            "vurdering": {
                                "vurdering": "OPPFYLT",
                                "årsak": "derfor"
                            }
                        }
                        """.trimIndent(),
                    )
                }

            assertEquals(HttpStatusCode.BadRequest, vilkårPostResponse.status)
            val pers = personId
            assertEquals(
                """
                {"type":"https://spillerom.ansatt.nav.no/validation/input","title":"Ugyldig format på Kode","status":400,"detail":null,"instance":"/v1/$pers/saksbehandlingsperioder/${saksbehandlingsperiode.id}/vilkaarsvurdering/ugyldig-KODE"}
                """.trimIndent(),
                vilkårPostResponse.bodyAsText(),
            )
        }

    @Test
    fun `Feil person+periode-kombo gir 400 for både GET,PUT og DELETE`() =
        vilkårAppTest { (daoer, saksbehandlingsperiode) ->
            val dennePersonId = personId
            val dennePeriodeId = saksbehandlingsperiode.id
            val annenPersonId = "pers2"

            val someBody =
                """
                {
                    "vurdering": "IKKE_OPPFYLT",
                    "årsak": "BOR_IKKE_I_NORGE"
                }
                """.trimIndent()

            daoer.personDao.opprettPerson("0101018888", annenPersonId)

            client
                .put(
                    "/v1/$annenPersonId/saksbehandlingsperioder/$dennePeriodeId/vilkaarsvurdering/BOR_I_NORGE",
                ) {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(someBody)
                }.apply {
                    assertEquals(HttpStatusCode.BadRequest, status, "feil person/periode-mix")
                }

            client
                .put(
                    "/v1/$dennePersonId/saksbehandlingsperioder/$dennePeriodeId/vilkaarsvurdering/BOR_I_NORGE",
                ) {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(someBody)
                }.apply {
                    assertEquals(HttpStatusCode.Created, status)
                }

            client
                .put(
                    "/v1/$annenPersonId/saksbehandlingsperioder/$dennePeriodeId/vilkaarsvurdering/BOR_I_NORGE",
                ) {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(someBody)
                }.apply {
                    assertEquals(HttpStatusCode.BadRequest, status, "feil person/periode-mix")
                }

            client
                .get("/v1/$annenPersonId/saksbehandlingsperioder/$dennePeriodeId/vilkaarsvurdering") {
                    bearerAuth(TestOppsett.userToken)
                }.apply {
                    assertEquals(HttpStatusCode.BadRequest, status)
                }

            client
                .delete(
                    "/v1/$annenPersonId/saksbehandlingsperioder/$dennePeriodeId/vilkaarsvurdering/BOR_I_NORGE",
                ) {
                    bearerAuth(TestOppsett.userToken)
                }.apply {
                    assertEquals(HttpStatusCode.BadRequest, status, "feil person/periode-mix")
                }

            client
                .get("/v1/$dennePersonId/saksbehandlingsperioder/$dennePeriodeId/vilkaarsvurdering") {
                    bearerAuth(TestOppsett.userToken)
                }.apply {
                    assertEquals(HttpStatusCode.OK, status)
                    assertEquals(
                        """[{"vurdering":"IKKE_OPPFYLT","årsak":"BOR_IKKE_I_NORGE","hovedspørsmål":"BOR_I_NORGE"}]""",
                        bodyAsText(),
                    )
                }

            client
                .delete(
                    "/v1/$dennePersonId/saksbehandlingsperioder/$dennePeriodeId/vilkaarsvurdering/BOR_I_NORGE",
                ) {
                    bearerAuth(TestOppsett.userToken)
                }.apply {
                    assertEquals(HttpStatusCode.NoContent, status)
                }
        }
}
