package no.nav.helse.bakrommet.bruker

import io.ktor.client.request.*
import io.ktor.client.statement.*
import no.nav.helse.bakrommet.TestOppsett.oAuthMock
import no.nav.helse.bakrommet.runApplicationTest
import org.junit.jupiter.api.Test

class BrukerRouteTest {
    @Test
    fun test1() {
        runApplicationTest {
            val response =
                client.get("/v1/bruker") {
                    bearerAuth(oAuthMock.token(grupper = listOf("GRUPPE_LES", "B", "C", "GRUPPE_BESLUTTER")))
                }

            println(response.bodyAsText())
        }
    }
}
