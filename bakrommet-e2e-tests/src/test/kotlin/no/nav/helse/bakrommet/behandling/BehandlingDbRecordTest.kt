package no.nav.helse.bakrommet.behandling

import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.helse.bakrommet.*
import no.nav.helse.bakrommet.api.dto.behandling.BehandlingDto
import no.nav.helse.bakrommet.person.NaturligIdent
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.hentAllePerioder
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.opprettBehandling
import no.nav.helse.bakrommet.testutils.`should equal`
import no.nav.helse.bakrommet.testutils.tidsstuttet
import no.nav.helse.bakrommet.util.somListe
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class BehandlingDbRecordTest {
    private companion object {
        val fnr = "01019012349"
        val personId = "65hth"
        val personPseudoId = UUID.nameUUIDFromBytes(personId.toByteArray())
    }

    @Test
    fun `oppretter saksbehandlingsperiode`() =
        runApplicationTest {
            it.personPseudoIdDao.opprettPseudoId(personPseudoId, NaturligIdent(fnr))

            // Opprett saksbehandlingsperiode via action
            val saksbehandlingsperiode =
                opprettBehandling(
                    personPseudoId.toString(),
                    LocalDate.parse("2023-01-01"),
                    LocalDate.parse("2023-01-31"),
                )
            saksbehandlingsperiode.fom.toString() `should equal` "2023-01-01"
            saksbehandlingsperiode.tom.toString() `should equal` "2023-01-31"
            saksbehandlingsperiode.naturligIdent `should equal` fnr
            saksbehandlingsperiode.opprettetAvNavIdent `should equal` "tullebruker"
            saksbehandlingsperiode.opprettetAvNavn `should equal` "Tulla Bruker"

            // Hent alle perioder via action
            val perioder = hentAllePerioder(personPseudoId)
            perioder.size `should equal` 1
            perioder `should equal` listOf(saksbehandlingsperiode)
            println(perioder)
        }

    @Test
    fun `henter alle perioder uten filter eller paginering`() {
        runApplicationTest {
            val fnr2 = "02029200000"
            val personId2 = "2ndnd"
            val personPseudoId2 = UUID.nameUUIDFromBytes(personId2.toByteArray())
            it.personPseudoIdDao.opprettPseudoId(personPseudoId, NaturligIdent(fnr))
            it.personPseudoIdDao.opprettPseudoId(personPseudoId2, NaturligIdent(fnr2))

            suspend fun lagPeriodePåPerson(personPseudoId: UUID) =
                client
                    .post("/v1/$personPseudoId/behandlinger") {
                        bearerAuth(TestOppsett.userToken)
                        contentType(ContentType.Application.Json)
                        setBody(
                            """
                            { "fom": "2023-01-01", "tom": "2023-01-31" }
                            """.trimIndent(),
                        )
                    }.body<BehandlingDto>()
            val periode1 =
                lagPeriodePåPerson(personPseudoId).also {
                    assertEquals(fnr, it.naturligIdent)
                }
            val periode2 =
                lagPeriodePåPerson(personPseudoId2).also {
                    assertEquals(fnr2, it.naturligIdent)
                }

            val absoluttAllePerioder: List<BehandlingDto> =
                client
                    .get("/v1/behandlinger") {
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
    fun `kan oppdatere skjæringstidspunkt`() =
        runApplicationTest {
            it.personPseudoIdDao.opprettPseudoId(personPseudoId, NaturligIdent(fnr))
            val opprettetPeriode =
                client
                    .post("/v1/$personPseudoId/behandlinger") {
                        bearerAuth(TestOppsett.userToken)
                        contentType(ContentType.Application.Json)
                        setBody(
                            """
                            { "fom": "2023-01-01", "tom": "2023-01-31" }
                            """.trimIndent(),
                        )
                    }.body<BehandlingDto>()

            // Oppdater skjæringstidspunkt
            val nyttSkjæringstidspunkt = "2023-01-15"
            val oppdatertPeriode =
                client
                    .put("/v1/$personPseudoId/behandlinger/${opprettetPeriode.id}/skjaeringstidspunkt") {
                        bearerAuth(TestOppsett.userToken)
                        contentType(ContentType.Application.Json)
                        setBody(
                            """
                            { "skjaeringstidspunkt": "$nyttSkjæringstidspunkt" }
                            """.trimIndent(),
                        )
                    }.body<BehandlingDto>()

            assertEquals(nyttSkjæringstidspunkt, oppdatertPeriode.skjæringstidspunkt.toString())
        }
}
