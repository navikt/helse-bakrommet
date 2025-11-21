package no.nav.helse.bakrommet.behandling.vilkaar

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.helse.bakrommet.Daoer
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.behandling.Behandling
import no.nav.helse.bakrommet.runApplicationTest
import no.nav.helse.bakrommet.testutils.truncateTidspunkt
import no.nav.helse.bakrommet.util.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VilkårRouteTest {
    private companion object {
        val fnr = "01019012349"
        val personId = "65hth"
    }

    fun vilkårAppTest(testBlock: suspend ApplicationTestBuilder.(Pair<Daoer, Behandling>) -> Unit) =
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

            val behandling: Behandling =
                objectMapper
                    .readValue(
                        response.bodyAsText(),
                        Behandling::class.java,
                    ).truncateTidspunkt()

            this.testBlock(it to behandling)
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
                            "underspørsmål": [],
                            "notat": "derfor"
                        }
                        """.trimIndent(),
                    )
                }

            assertEquals(HttpStatusCode.Created, vilkårPutResponse.status)
            val responseBody = vilkårPutResponse.bodyAsText()
            assertEquals("OPPFYLT", objectMapper.readValue<Map<String, Any>>(responseBody)["vurdering"])
            assertEquals("BOR_I_NORGE", objectMapper.readValue<Map<String, Any>>(responseBody)["hovedspørsmål"])
            assertEquals("derfor", objectMapper.readValue<Map<String, Any>>(responseBody)["notat"])
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
                            "underspørsmål": [],
                            "notat": "derfor"
                        }
                        """.trimIndent(),
                    )
                }.apply {
                    assertEquals(HttpStatusCode.Created, status)
                    val responseBody = bodyAsText()
                    assertEquals("OPPFYLT", objectMapper.readValue<Map<String, Any>>(responseBody)["vurdering"])
                    assertEquals("BOR_I_NORGE", objectMapper.readValue<Map<String, Any>>(responseBody)["hovedspørsmål"])
                    assertEquals("derfor", objectMapper.readValue<Map<String, Any>>(responseBody)["notat"])
                }

            client
                .get("/v1/$personId/saksbehandlingsperioder/${saksbehandlingsperiode.id}/vilkaarsvurdering") {
                    bearerAuth(TestOppsett.userToken)
                }.apply {
                    assertEquals(HttpStatusCode.OK, status)
                    val responseBody = bodyAsText()
                    val list = objectMapper.readValue<List<Map<String, Any>>>(responseBody)
                    assertEquals(1, list.size)
                    assertEquals("OPPFYLT", list[0]["vurdering"])
                    assertEquals("BOR_I_NORGE", list[0]["hovedspørsmål"])
                    assertEquals("derfor", list[0]["notat"])
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
                            "underspørsmål": [],
                            "notat": "BOR_IKKE_I_NORGE"
                        }
                        """.trimIndent(),
                    )
                }.apply {
                    assertEquals(HttpStatusCode.OK, status)
                    val responseBody = bodyAsText()
                    assertEquals("IKKE_OPPFYLT", objectMapper.readValue<Map<String, Any>>(responseBody)["vurdering"])
                    assertEquals("BOR_I_NORGE", objectMapper.readValue<Map<String, Any>>(responseBody)["hovedspørsmål"])
                    assertEquals("BOR_IKKE_I_NORGE", objectMapper.readValue<Map<String, Any>>(responseBody)["notat"])
                }

            client
                .get("/v1/$personId/saksbehandlingsperioder/${saksbehandlingsperiode.id}/vilkaarsvurdering") {
                    bearerAuth(TestOppsett.userToken)
                }.apply {
                    assertEquals(HttpStatusCode.OK, status)
                    val responseBody = bodyAsText()
                    val list = objectMapper.readValue<List<Map<String, Any>>>(responseBody)
                    assertEquals(1, list.size)
                    assertEquals("IKKE_OPPFYLT", list[0]["vurdering"])
                    assertEquals("BOR_I_NORGE", list[0]["hovedspørsmål"])
                    assertEquals("BOR_IKKE_I_NORGE", list[0]["notat"])
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
                            "vurdering": "IKKE_RELEVANT",
                            "underspørsmål": []
                        }
                        """.trimIndent(),
                    )
                }.apply {
                    assertEquals(HttpStatusCode.Created, status)
                    val responseBody = bodyAsText()
                    assertEquals("IKKE_RELEVANT", objectMapper.readValue<Map<String, Any>>(responseBody)["vurdering"])
                    assertEquals("ET_VILKÅR_TIL", objectMapper.readValue<Map<String, Any>>(responseBody)["hovedspørsmål"])
                }

            client
                .get("/v1/$personId/saksbehandlingsperioder/${saksbehandlingsperiode.id}/vilkaarsvurdering") {
                    bearerAuth(TestOppsett.userToken)
                }.apply {
                    assertEquals(HttpStatusCode.OK, status)
                    val responseBody = bodyAsText()
                    val list = objectMapper.readValue<List<Map<String, Any>>>(responseBody)
                    assertEquals(2, list.size)
                    val set = list.map { mapOf("vurdering" to (it["vurdering"] as String), "hovedspørsmål" to (it["hovedspørsmål"] as String), "notat" to (it["notat"] as? String)) }.toSet()
                    assertEquals(
                        setOf(
                            mapOf(
                                "vurdering" to "IKKE_OPPFYLT",
                                "notat" to "BOR_IKKE_I_NORGE",
                                "hovedspørsmål" to "BOR_I_NORGE",
                            ),
                            mapOf(
                                "vurdering" to "IKKE_RELEVANT",
                                "hovedspørsmål" to "ET_VILKÅR_TIL",
                                "notat" to null,
                            ),
                        ),
                        set,
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
                    assertEquals(HttpStatusCode.BadRequest, status, "skal gi 400 når koden ikke fantes")
                }

            client
                .get("/v1/$personId/saksbehandlingsperioder/${saksbehandlingsperiode.id}/vilkaarsvurdering") {
                    bearerAuth(TestOppsett.userToken)
                }.apply {
                    assertEquals(HttpStatusCode.OK, status)
                    val responseBody = bodyAsText()
                    val list = objectMapper.readValue<List<Map<String, Any>>>(responseBody)
                    assertEquals(1, list.size)
                    assertEquals("IKKE_RELEVANT", list[0]["vurdering"])
                    assertEquals("ET_VILKÅR_TIL", list[0]["hovedspørsmål"])
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
                            "vurdering": "OPPFYLT",
                            "underspørsmål": []
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
                    "underspørsmål": [],
                    "notat": "BOR_IKKE_I_NORGE"
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
                    val responseBody = bodyAsText()
                    val list = objectMapper.readValue<List<Map<String, Any>>>(responseBody)
                    assertEquals(1, list.size)
                    assertEquals("IKKE_OPPFYLT", list[0]["vurdering"])
                    assertEquals("BOR_I_NORGE", list[0]["hovedspørsmål"])
                    assertEquals("BOR_IKKE_I_NORGE", list[0]["notat"])
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
