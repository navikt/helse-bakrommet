package no.nav.helse.bakrommet.saksbehandlingsperiode

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.helse.bakrommet.*
import no.nav.helse.bakrommet.ainntekt.AInntektMock
import no.nav.helse.bakrommet.ainntekt.etInntektSvar
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadMock
import no.nav.helse.bakrommet.sykepengesoknad.enSøknad
import no.nav.helse.bakrommet.testutils.`should equal`
import no.nav.helse.bakrommet.testutils.somListe
import no.nav.helse.bakrommet.testutils.truncateTidspunkt
import no.nav.helse.bakrommet.util.asJsonNode
import no.nav.helse.bakrommet.util.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class SaksbehandlingsperiodeTest {
    companion object {
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

            val saksbehandlingsperiode: Saksbehandlingsperiode =
                objectMapper.readValue(
                    response.bodyAsText(),
                    Saksbehandlingsperiode::class.java,
                ).truncateTidspunkt()
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
    fun `oppretter saksbehandlingsperiode og henter dokumenter`() {
        val søknad1 =
            UUID.randomUUID().toString().let {
                it to enSøknad(fnr = fnr, id = it)
            }
        val søknad2 =
            UUID.randomUUID().toString().let {
                it to enSøknad(fnr = fnr, id = it)
            }
        val inntekter = fnr to etInntektSvar(fnr = fnr)
        runApplicationTest(
            sykepengesoknadBackendClient =
                SykepengesoknadMock.sykepengersoknadBackendClientMock(
                    søknadIdTilSvar =
                        mapOf(
                            søknad1,
                            søknad2,
                        ),
                ),
            aInntektClient =
                AInntektMock.aInntektClientMock(
                    fnrTilSvar = mapOf(inntekter),
                ),
        ) {
            it.personDao.opprettPerson(fnr, personId)
            val response =
                client.post("/v1/$personId/saksbehandlingsperioder") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        { "fom": "2023-01-01", "tom": "2023-01-31", "søknader": ["${søknad1.first}", "${søknad2.first}"] }
                        """.trimIndent(),
                    )
                }
            assertEquals(201, response.status.value)

            val allePerioder =
                client.get("/v1/$personId/saksbehandlingsperioder") {
                    bearerAuth(TestOppsett.userToken)
                }
            assertEquals(200, allePerioder.status.value)
            val perioder: List<Saksbehandlingsperiode> = allePerioder.bodyAsText().somListe()

            perioder.size `should equal` 1
            val periode = perioder.first()
            val dokumenter = it.dokumentDao.hentDokumenterFor(periode.id)

            assertEquals(3, dokumenter.size)
            dokumenter.find { it.eksternId == søknad1.first }!!.also {
                assertEquals(søknad1.second.asJsonNode(), it.innhold.asJsonNode())
            }
            dokumenter.find { it.eksternId == søknad2.first }!!.also { dok ->
                assertEquals(søknad2.second.asJsonNode(), dok.innhold.asJsonNode())
                assertTrue(
                    listOf(
                        "SykepengesoknadBackendClient.kt",
                        "DokumentHenter.kt",
                        "sykepengesoknad-backend/api/v3/soknader/${søknad2.first}",
                    ).all { spor ->
                        // println(dok.request.kilde)
                        dok.request.kilde.contains(spor)
                    },
                    "Fant ikke alt som var forventet i ${dok.request.kilde}",
                ) // TODO: Outsource til egen dedikert test?
            }
            dokumenter.find { it.dokumentType == "ainntekt828" }!!.also { dok ->
                assertEquals(inntekter.second.asJsonNode(), dok.innhold.asJsonNode())
                assertTrue(
                    listOf(
                        "AInntektClient.kt",
                        "DokumentHenter.kt",
                        "/rs/api/v1/hentinntektliste",
                        "8-28",
                        fnr,
                        "imagename",
                    ).all { spor ->
                        // println(dok.request.kilde)
                        dok.request.kilde.contains(spor)
                    },
                    "Fant ikke alt som var forventet i ${dok.request.kilde}",
                ) // TODO: Outsource til egen dedikert test?
            }
        }
    }
}
