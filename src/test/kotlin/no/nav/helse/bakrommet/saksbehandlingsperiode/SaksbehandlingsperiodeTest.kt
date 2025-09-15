package no.nav.helse.bakrommet.saksbehandlingsperiode

import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.helse.bakrommet.*
import no.nav.helse.bakrommet.testutils.`should equal`
import no.nav.helse.bakrommet.testutils.tidsstuttet
import no.nav.helse.bakrommet.testutils.truncateTidspunkt
import no.nav.helse.bakrommet.util.somListe
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SaksbehandlingsperiodeTest {
    private companion object {
        val fnr = "01019012349"
        val personId = "65hth"
    }

    @Test
    fun `oppretter saksbehandlingsperiode`() =
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

            val saksbehandlingsperiode = response.body<Saksbehandlingsperiode>().truncateTidspunkt()
            saksbehandlingsperiode.fom.toString() `should equal` "2023-01-01"
            saksbehandlingsperiode.tom.toString() `should equal` "2023-01-31"
            saksbehandlingsperiode.spilleromPersonId `should equal` personId
            saksbehandlingsperiode.opprettetAvNavIdent `should equal` "tullebruker"
            saksbehandlingsperiode.opprettetAvNavn `should equal` "Tulla Bruker"

            val allePerioder =
                client.get("/v1/$personId/saksbehandlingsperioder") {
                    bearerAuth(TestOppsett.userToken)
                }
            assertEquals(200, allePerioder.status.value)
            val perioder: List<Saksbehandlingsperiode> = allePerioder.bodyAsText().somListe()
            perioder.size `should equal` 1
            perioder.map { it.truncateTidspunkt() } `should equal` listOf(saksbehandlingsperiode)
            println(perioder)
        }

    @Test
    fun `henter alle perioder uten filter eller paginering`() {
        runApplicationTest {
            val fnr2 = "02029200000"
            val personId2 = "2ndnd"
            it.personDao.opprettPerson(fnr, personId)
            it.personDao.opprettPerson(fnr2, personId2)

            suspend fun lagPeriodePåPerson(personId: String) =
                client.post("/v1/$personId/saksbehandlingsperioder") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        { "fom": "2023-01-01", "tom": "2023-01-31" }
                        """.trimIndent(),
                    )
                }.body<Saksbehandlingsperiode>()
            val periode1 =
                lagPeriodePåPerson(personId).also {
                    assertEquals(personId, it.spilleromPersonId)
                }
            val periode2 =
                lagPeriodePåPerson(personId2).also {
                    assertEquals(personId2, it.spilleromPersonId)
                }

            val absoluttAllePerioder: List<Saksbehandlingsperiode> =
                client.get("/v1/saksbehandlingsperioder") {
                    bearerAuth(TestOppsett.userToken)
                }.let { resp ->
                    assertEquals(200, resp.status.value)
                    resp.bodyAsText().somListe()
                }
            assertEquals(
                listOf(periode1, periode2).tidsstuttet().toSet(),
                absoluttAllePerioder.tidsstuttet().toSet(),
            )
        }
    }

    @Test
    fun `kan oppdatere individuell begrunnelse`() =
        runApplicationTest {
            it.personDao.opprettPerson(fnr, personId)
            val opprettetPeriode =
                client.post("/v1/$personId/saksbehandlingsperioder") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        { "fom": "2023-01-01", "tom": "2023-01-31" }
                        """.trimIndent(),
                    )
                }.body<Saksbehandlingsperiode>()

            // Oppdater individuell begrunnelse
            val nyBegrunnelse = "Spesielle omstendigheter som krever individuell vurdering"
            val oppdatertPeriode =
                client.put("/v1/$personId/saksbehandlingsperioder/${opprettetPeriode.id}/individuell-begrunnelse") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        { "individuellBegrunnelse": "$nyBegrunnelse" }
                        """.trimIndent(),
                    )
                }.body<Saksbehandlingsperiode>()

            assertEquals(nyBegrunnelse, oppdatertPeriode.individuellBegrunnelse)

            // Nullstill individuell begrunnelse
            val nullstiltPeriode =
                client.put("/v1/$personId/saksbehandlingsperioder/${opprettetPeriode.id}/individuell-begrunnelse") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        { "individuellBegrunnelse": null }
                        """.trimIndent(),
                    )
                }.body<Saksbehandlingsperiode>()

            assertEquals(null, nullstiltPeriode.individuellBegrunnelse)
        }

    @Test
    fun `kan oppdatere skjæringstidspunkt`() =
        runApplicationTest {
            it.personDao.opprettPerson(fnr, personId)
            val opprettetPeriode =
                client.post("/v1/$personId/saksbehandlingsperioder") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        { "fom": "2023-01-01", "tom": "2023-01-31" }
                        """.trimIndent(),
                    )
                }.body<Saksbehandlingsperiode>()

            // Oppdater skjæringstidspunkt
            val nyttSkjæringstidspunkt = "2023-01-15"
            val oppdatertPeriode =
                client.put("/v1/$personId/saksbehandlingsperioder/${opprettetPeriode.id}/skjaeringstidspunkt") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        { "skjaeringstidspunkt": "$nyttSkjæringstidspunkt" }
                        """.trimIndent(),
                    )
                }.body<Saksbehandlingsperiode>()

            assertEquals(nyttSkjæringstidspunkt, oppdatertPeriode.skjæringstidspunkt.toString())

            // Nullstill skjæringstidspunkt
            val nullstiltPeriode =
                client.put("/v1/$personId/saksbehandlingsperioder/${opprettetPeriode.id}/skjaeringstidspunkt") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        { "skjaeringstidspunkt": null }
                        """.trimIndent(),
                    )
                }.body<Saksbehandlingsperiode>()

            assertEquals(null, nullstiltPeriode.skjæringstidspunkt)
        }
}
