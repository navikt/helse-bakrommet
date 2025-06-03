package no.nav.helse.bakrommet.saksbehandlingsperiode.vilkaar

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.helse.bakrommet.Daoer
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.runApplicationTest
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeTest
import no.nav.helse.bakrommet.testutils.somListe
import no.nav.helse.bakrommet.testutils.truncateTidspunkt
import no.nav.helse.bakrommet.util.objectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class VilkårRouteTest {
    fun vilkårAppTest(testBlock: suspend ApplicationTestBuilder.(Pair<Daoer, Saksbehandlingsperiode>) -> Unit) =
        runApplicationTest {
            it.personDao.opprettPerson(SaksbehandlingsperiodeTest.fnr, SaksbehandlingsperiodeTest.personId)
            val response =
                client.post("/v1/${SaksbehandlingsperiodeTest.personId}/saksbehandlingsperioder") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        { "fom": "2023-01-01", "tom": "2023-01-31" }
                        """.trimIndent(),
                    )
                }
            Assertions.assertEquals(201, response.status.value)

            val saksbehandlingsperiode: Saksbehandlingsperiode =
                objectMapper.readValue(
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
                    "/v1/${SaksbehandlingsperiodeTest.personId}/saksbehandlingsperioder/${saksbehandlingsperiode.id}/vilkår/BOR_I_NORGE",
                ) {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {
                            "vurdering": {
                                "status": "OPPFYLT",
                                "fordi": "derfor"
                            }
                        }
                        """.trimIndent(),
                    )
                }

            assertEquals(HttpStatusCode.Created, vilkårPutResponse.status)
            assertEquals(
                """{"kode":"BOR_I_NORGE","vurdering":{"status":"OPPFYLT","fordi":"derfor"}}""",
                vilkårPutResponse.bodyAsText(),
            )
        }

    @Test
    fun `oppretter, endrer, legger til, henter og sletter vurdert vilkår på saksbehandlingsperiode`() =
        vilkårAppTest { (daoer, saksbehandlingsperiode) ->
            saksbehandlingsperiode.id
            client.put(
                "/v1/${SaksbehandlingsperiodeTest.personId}/saksbehandlingsperioder/${saksbehandlingsperiode.id}/vilkår/BOR_I_NORGE",
            ) {
                bearerAuth(TestOppsett.userToken)
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                        "vurdering": {
                            "status": "OPPFYLT",
                            "fordi": "derfor"
                        }
                    }
                    """.trimIndent(),
                )
            }.apply {
                assertEquals(HttpStatusCode.Created, status)
                assertEquals(
                    """{"kode":"BOR_I_NORGE","vurdering":{"status":"OPPFYLT","fordi":"derfor"}}""",
                    bodyAsText(),
                )
            }

            client.get("/v1/${SaksbehandlingsperiodeTest.personId}/saksbehandlingsperioder/${saksbehandlingsperiode.id}/vilkår") {
                bearerAuth(TestOppsett.userToken)
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals(
                    """[{"kode":"BOR_I_NORGE","vurdering":{"status":"OPPFYLT","fordi":"derfor"}}]""",
                    bodyAsText(),
                )
            }

            client.put(
                "/v1/${SaksbehandlingsperiodeTest.personId}/saksbehandlingsperioder/${saksbehandlingsperiode.id}/vilkår/BOR_I_NORGE",
            ) {
                bearerAuth(TestOppsett.userToken)
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                        "vurdering": {
                            "status": "IKKE_OPPFYLT",
                            "fordi": "BOR_IKKE_I_NORGE"
                        }
                    }
                    """.trimIndent(),
                )
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals(
                    """{"kode":"BOR_I_NORGE","vurdering":{"status":"IKKE_OPPFYLT","fordi":"BOR_IKKE_I_NORGE"}}""",
                    bodyAsText(),
                )
            }

            client.get("/v1/${SaksbehandlingsperiodeTest.personId}/saksbehandlingsperioder/${saksbehandlingsperiode.id}/vilkår") {
                bearerAuth(TestOppsett.userToken)
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals(
                    """[{"kode":"BOR_I_NORGE","vurdering":{"status":"IKKE_OPPFYLT","fordi":"BOR_IKKE_I_NORGE"}}]""",
                    bodyAsText(),
                )
            }

            client.put(
                "/v1/${SaksbehandlingsperiodeTest.personId}/saksbehandlingsperioder/${saksbehandlingsperiode.id}/vilkår/ET_VILKÅR_TIL",
            ) {
                bearerAuth(TestOppsett.userToken)
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                        "vurdering": {
                            "status": "IKKE_AKTUELT"
                        }
                    }
                    """.trimIndent(),
                )
            }.apply {
                assertEquals(HttpStatusCode.Created, status)
                assertEquals(
                    """{"kode":"ET_VILKÅR_TIL","vurdering":{"status":"IKKE_AKTUELT"}}""",
                    bodyAsText(),
                )
            }

            client.get("/v1/${SaksbehandlingsperiodeTest.personId}/saksbehandlingsperioder/${saksbehandlingsperiode.id}/vilkår") {
                bearerAuth(TestOppsett.userToken)
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals(
                    setOf(
                        VurdertVilkår(
                            kode = "BOR_I_NORGE",
                            vurdering =
                                objectMapper.createObjectNode().apply {
                                    put("status", "IKKE_OPPFYLT")
                                    put("fordi", "BOR_IKKE_I_NORGE")
                                },
                        ),
                        VurdertVilkår(
                            kode = "ET_VILKÅR_TIL",
                            vurdering =
                                objectMapper.createObjectNode().apply {
                                    put("status", "IKKE_AKTUELT")
                                },
                        ),
                    ),
                    bodyAsText().somListe<VurdertVilkår>().toSet(),
                )
            }

            client.delete(
                "/v1/${SaksbehandlingsperiodeTest.personId}/saksbehandlingsperioder/${saksbehandlingsperiode.id}/vilkår/BOR_I_NORGE",
            ) {
                bearerAuth(TestOppsett.userToken)
            }.apply {
                assertEquals(HttpStatusCode.NoContent, status, "skal gi 204 når koden fantes og er blitt slettet")
            }

            client.delete(
                "/v1/${SaksbehandlingsperiodeTest.personId}/saksbehandlingsperioder/${saksbehandlingsperiode.id}/vilkår/BOR_I_NORGE",
            ) {
                bearerAuth(TestOppsett.userToken)
            }.apply {
                assertEquals(HttpStatusCode.NotFound, status, "skal gi 404 når koden ikke fantes")
            }

            client.get("/v1/${SaksbehandlingsperiodeTest.personId}/saksbehandlingsperioder/${saksbehandlingsperiode.id}/vilkår") {
                bearerAuth(TestOppsett.userToken)
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals(
                    setOf(
                        VurdertVilkår(
                            kode = "ET_VILKÅR_TIL",
                            vurdering =
                                objectMapper.createObjectNode().apply {
                                    put("status", "IKKE_AKTUELT")
                                },
                        ),
                    ),
                    bodyAsText().somListe<VurdertVilkår>().toSet(),
                )
            }
        }

    @Test
    fun `ugyldig kode-format gir 400 med beskrivelse`() =
        vilkårAppTest { (daoer, saksbehandlingsperiode) ->
            saksbehandlingsperiode.id
            val vilkårPostResponse =
                client.put(
                    "/v1/${SaksbehandlingsperiodeTest.personId}/saksbehandlingsperioder/${saksbehandlingsperiode.id}/vilkår/ugyldig-KODE",
                ) {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {
                            "vurdering": {
                                "status": "OPPFYLT",
                                "fordi": "derfor"
                            }
                        }
                        """.trimIndent(),
                    )
                }

            assertEquals(HttpStatusCode.BadRequest, vilkårPostResponse.status)
            val pers = SaksbehandlingsperiodeTest.personId
            assertEquals(
                """
                {"type":"https://spillerom.ansatt.nav.no/validation/input","title":"Ugyldig format på Kode","status":400,"detail":null,"instance":"/v1/$pers/saksbehandlingsperioder/${saksbehandlingsperiode.id}/vilkår/ugyldig-KODE"}
                """.trimIndent(),
                vilkårPostResponse.bodyAsText(),
            )
        }
}
