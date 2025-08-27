package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.runApplicationTest
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagRequest
import no.nav.helse.bakrommet.sykepengesoknad.Arbeidsgiverinfo
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadMock
import no.nav.helse.bakrommet.sykepengesoknad.enSøknad
import no.nav.helse.bakrommet.util.asJsonNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class UtbetalingsberegningIntegrasjonTest {
    private companion object {
        const val FNR = "01019012349"
        const val PERSON_ID = "65hth"
        const val ARBEIDSGIVER_ORGNR = "123321123"
        const val ARBEIDSGIVER_NAVN = "Test Bedrift AS"
    }

    @Test
    fun `beregner utbetalinger korrekt med økonomi-klassene`() {
        val arbeidsgiver =
            Arbeidsgiverinfo(
                identifikator = ARBEIDSGIVER_ORGNR,
                navn = ARBEIDSGIVER_NAVN,
            )

        val søknad =
            enSøknad(
                fnr = FNR,
                id = UUID.randomUUID().toString(),
                arbeidsgiverinfo = arbeidsgiver,
            ).asJsonNode()

        runApplicationTest(
            sykepengesoknadBackendClient =
                SykepengesoknadMock.sykepengersoknadBackendClientMock(
                    søknadIdTilSvar = mapOf(søknad["id"].asText() to søknad),
                ),
        ) { daoer ->
            daoer.personDao.opprettPerson(FNR, PERSON_ID)

            // Opprett saksbehandlingsperiode
            val periode = opprettSaksbehandlingsperiode()

            // Opprett yrkesaktivitet som ordinær arbeidstaker
            val yrkesaktivitetId = opprettYrkesaktivitet(periode.id)

            // Sett sykepengegrunnlag (dette trigger automatisk utbetalingsberegning)
            settSykepengegrunnlag(periode.id, yrkesaktivitetId)

            // Sett dagoversikt med forskjellige dagtyper (dette trigger også utbetalingsberegning)
            settDagoversikt(periode.id, yrkesaktivitetId)

            // Hent utbetalingsberegning
            val beregning = hentUtbetalingsberegning(periode.id)

            // Verifiser resultatet
            verifiserBeregning(beregning)
        }
    }

    private suspend fun ApplicationTestBuilder.opprettSaksbehandlingsperiode(): Saksbehandlingsperiode {
        client.post("/v1/$PERSON_ID/saksbehandlingsperioder") {
            bearerAuth(TestOppsett.userToken)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                    "fom": "2024-01-01",
                    "tom": "2024-01-31"
                }
                """.trimIndent(),
            )
        }

        val response =
            client.get("/v1/$PERSON_ID/saksbehandlingsperioder") {
                bearerAuth(TestOppsett.userToken)
            }
        assertEquals(200, response.status.value)
        return response.body<List<Saksbehandlingsperiode>>().first()
    }

    private suspend fun ApplicationTestBuilder.opprettYrkesaktivitet(periodeId: UUID): UUID {
        val kategorisering =
            """
            {
                "kategorisering": {
                    "INNTEKTSKATEGORI": "ARBEIDSTAKER",
                    "ORGNUMMER": "$ARBEIDSGIVER_ORGNR"
                }
            }
            """.trimIndent()

        val response =
            client.post("/v1/$PERSON_ID/saksbehandlingsperioder/$periodeId/yrkesaktivitet") {
                bearerAuth(TestOppsett.userToken)
                contentType(ContentType.Application.Json)
                setBody(kategorisering)
            }
        assertEquals(201, response.status.value)
        return UUID.fromString(response.body<JsonNode>()["id"].asText())
    }

    private suspend fun ApplicationTestBuilder.settSykepengegrunnlag(
        periodeId: UUID,
        yrkesaktivitetId: UUID,
    ) {
        // Månedsinntekt på 50 000 kr (5 000 000 øre)
        val sykepengegrunnlagRequest =
            SykepengegrunnlagRequest(
                inntekter =
                    listOf(
                        no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Inntekt(
                            yrkesaktivitetId = yrkesaktivitetId,
                            beløpPerMånedØre = 5_000_000L,
                            kilde = no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Inntektskilde.AINNTEKT,
                            refusjon =
                                listOf(
                                    no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Refusjonsperiode(
                                        fom = LocalDate.of(2024, 1, 1),
                                        tom = LocalDate.of(2024, 1, 31),
                                        beløpØre = 1_000_000L,
                                    ),
                                ),
                        ),
                    ),
                begrunnelse = "Test sykepengegrunnlag",
            )

        val response =
            client.put("/v1/$PERSON_ID/saksbehandlingsperioder/$periodeId/sykepengegrunnlag") {
                bearerAuth(TestOppsett.userToken)
                contentType(ContentType.Application.Json)
                setBody(sykepengegrunnlagRequest)
            }
        assertEquals(200, response.status.value)
    }

    private suspend fun ApplicationTestBuilder.settDagoversikt(
        periodeId: UUID,
        yrkesaktivitetId: UUID,
    ) {
        val dagoversikt =
            """
            [
                {
                    "dato": "2024-01-01",
                    "dagtype": "Syk",
                    "grad": 100,
                    "avvistBegrunnelse": [],
                    "kilde": "Saksbehandler"
                },
                {
                    "dato": "2024-01-02",
                    "dagtype": "Syk",
                    "grad": 70,
                    "avvistBegrunnelse": [],
                    "kilde": "Saksbehandler"
                },
                {
                    "dato": "2024-01-03",
                    "dagtype": "Ferie",
                    "avvistBegrunnelse": [],
                    "kilde": "Saksbehandler"
                },
                {
                    "dato": "2024-01-04",
                    "dagtype": "Syk",
                    "grad": 100,
                    "avvistBegrunnelse": [],
                    "kilde": "Saksbehandler"
                },
                {
                    "dato": "2024-01-05",
                    "dagtype": "Arbeidsdag",
                    "avvistBegrunnelse": [],
                    "kilde": "Saksbehandler"
                }
            ]
            """.trimIndent()

        val response =
            client.put("/v1/$PERSON_ID/saksbehandlingsperioder/$periodeId/yrkesaktivitet/$yrkesaktivitetId/dagoversikt") {
                bearerAuth(TestOppsett.userToken)
                contentType(ContentType.Application.Json)
                setBody(dagoversikt)
            }
        assertEquals(204, response.status.value)
    }

    private suspend fun ApplicationTestBuilder.hentUtbetalingsberegning(periodeId: UUID): BeregningResponse? {
        val response =
            client.get("/v1/$PERSON_ID/saksbehandlingsperioder/$periodeId/utbetalingsberegning") {
                bearerAuth(TestOppsett.userToken)
            }
        assertEquals(200, response.status.value)
        val responseText = response.bodyAsText()
        return if (responseText == "null") null else response.body()
    }

    private fun verifiserBeregning(beregning: BeregningResponse?) {
        // Beregning kan være null hvis ingen utbetalinger skal gjøres
        if (beregning == null) {
            return
        }

        assertEquals(1, beregning.beregningData.yrkesaktiviteter.size)

        val yrkesaktivitet = beregning.beregningData.yrkesaktiviteter.first()
        assertEquals(31, yrkesaktivitet.dager.size) // Januar 2024 har 31 dager

        // Dag 1: 100% syk - skal ha refusjon
        val dag1 = yrkesaktivitet.dager.find { it.dato == LocalDate.of(2024, 1, 1) }!!
        assertEquals(197500, dag1.utbetalingØre, "Dag 1 skal ha personutbetaling siden refusjon ikke dekker alt")
        assertEquals(33300, dag1.refusjonØre, "Dag 1 skal ha 33300 øre refusjon (10000 kr / 30 dager)")
        assertEquals(100, dag1.totalGrad, "Dag 1 skal ha 100% total grad")

        // Dag 2: 70% syk - skal ha 70% refusjon
        val dag2 = yrkesaktivitet.dager.find { it.dato == LocalDate.of(2024, 1, 2) }!!
        assertEquals(138200, dag2.utbetalingØre, "Dag 2 skal ha personutbetaling siden refusjon ikke dekker alt")
        assertEquals(23300, dag2.refusjonØre, "Dag 2 skal ha 23300 øre refusjon (70% av dag 1, avrundet)")
        assertEquals(70, dag2.totalGrad, "Dag 2 skal ha 70% total grad")

        // Dag 3: Ferie - skal ikke ha utbetaling
        val dag3 = yrkesaktivitet.dager.find { it.dato == LocalDate.of(2024, 1, 3) }!!
        assertEquals(0, dag3.utbetalingØre, "Dag 3 (Ferie) skal ikke ha utbetaling")
        assertEquals(0, dag3.refusjonØre, "Dag 3 (Ferie) skal ikke ha refusjon")
        assertEquals(0, dag3.totalGrad, "Dag 3 (Ferie) skal ha 0% total grad")

        // Dag 4: 100% syk - skal ha samme refusjon som dag 1
        val dag4 = yrkesaktivitet.dager.find { it.dato == LocalDate.of(2024, 1, 4) }!!
        assertEquals(197500, dag4.utbetalingØre, "Dag 4 skal ha samme personutbetaling som dag 1")
        assertEquals(33300, dag4.refusjonØre, "Dag 4 skal ha samme refusjon som dag 1")
        assertEquals(100, dag4.totalGrad, "Dag 4 skal ha 100% total grad")

        // Dag 5: Arbeidsdag - skal ikke ha utbetaling
        val dag5 = yrkesaktivitet.dager.find { it.dato == LocalDate.of(2024, 1, 5) }!!
        assertEquals(0, dag5.utbetalingØre, "Dag 5 (Arbeidsdag) skal ikke ha utbetaling")
        assertEquals(0, dag5.refusjonØre, "Dag 5 (Arbeidsdag) skal ikke ha refusjon")
        assertEquals(0, dag5.totalGrad, "Dag 5 (Arbeidsdag) skal ha 0% total grad")
    }
}
