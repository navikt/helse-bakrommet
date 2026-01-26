package no.nav.helse.bakrommet.e2e.auth

import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.helse.bakrommet.e2e.TestOppsett
import no.nav.helse.bakrommet.e2e.runApplicationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RolleAutoriseringTest {
    @Test
    fun `kan ikke opprette saksbehandlingsperiode med LES-rolle`() =
        runApplicationTest {
            val response =
                client.post("/v1/12345/behandlinger") {
                    bearerAuth(TestOppsett.oAuthMock.token(grupper = listOf("GRUPPE_LES")))
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        { "fom": "2023-01-01", "tom": "2023-01-31" }
                        """.trimIndent(),
                    )
                }

            assertEquals(403, response.status.value)
        }

    @Test
    fun `kan ikke sette vilkår med LES-rolle`() =
        runApplicationTest {
            // Forsøk å sette vilkår med LES-rolle
            val vilkårResponse =
                client.put(
                    "/v1/12345/behandlinger/d12266e7-b88b-403e-883d-de24205f526c/vilkaarsvurdering/BOR_I_NORGE",
                ) {
                    bearerAuth(TestOppsett.oAuthMock.token(grupper = listOf("GRUPPE_LES")))
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {
                            "vurdering": "OPPFYLT",
                            "årsak": "bor i Norge"
                        }
                        """.trimIndent(),
                    )
                }

            assertEquals(403, vilkårResponse.status.value)
        }
}
