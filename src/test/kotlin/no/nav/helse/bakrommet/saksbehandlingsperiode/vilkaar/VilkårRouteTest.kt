package no.nav.helse.bakrommet.saksbehandlingsperiode.vilkaar

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.helse.bakrommet.Daoer
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.runApplicationTest
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeTest
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
    fun `oppretter vurderte vilkår på saksbehandlingsperiode`() =
        vilkårAppTest { (daoer, saksbehandlingsperiode) ->
            saksbehandlingsperiode.id
            val vilkårPostResponse =
                client.post("/v1/${SaksbehandlingsperiodeTest.personId}/saksbehandlingsperioder/${saksbehandlingsperiode.id}/vilkår") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        [
                            {
                                "vilkårKode": "BOR_I_NORGE",
                                "status": "OPPFYLT",
                                "fordi": "derfor"
                            }
                        ]
                        """.trimIndent(),
                    )
                }

            assertEquals(HttpStatusCode.OK, vilkårPostResponse.status)
        }

    @Test
    fun `ugyldig kode-format gir 400 med beskrivelse`() =
        vilkårAppTest { (daoer, saksbehandlingsperiode) ->
            saksbehandlingsperiode.id
            val vilkårPostResponse =
                client.post("/v1/${SaksbehandlingsperiodeTest.personId}/saksbehandlingsperioder/${saksbehandlingsperiode.id}/vilkår") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        [
                            {
                                "vilkårKode": "BOR//NORGE",
                                "status": "OPPFYLT",
                                "fordi": "derfor"
                            }
                        ]
                        """.trimIndent(),
                    )
                }

            assertEquals(HttpStatusCode.BadRequest, vilkårPostResponse.status)
            val pers = SaksbehandlingsperiodeTest.personId
            assertEquals(
                """
                {"type":"https://spillerom.ansatt.nav.no/validation/input","title":"Ugyldig format på Kode","status":400,"detail":null,"instance":"/v1/$pers/saksbehandlingsperioder/${saksbehandlingsperiode.id}/vilkår"}
                """.trimIndent(),
                vilkårPostResponse.bodyAsText(),
            )
        }
}
