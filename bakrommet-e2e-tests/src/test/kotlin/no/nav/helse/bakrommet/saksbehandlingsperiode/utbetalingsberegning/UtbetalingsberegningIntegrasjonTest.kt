package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.TestOppsett.oAuthMock
import no.nav.helse.bakrommet.godkjenn
import no.nav.helse.bakrommet.kafka.SaksbehandlingsperiodeKafkaDto
import no.nav.helse.bakrommet.runApplicationTest
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagRequest
import no.nav.helse.bakrommet.sendTilBeslutning
import no.nav.helse.bakrommet.sykepengesoknad.Arbeidsgiverinfo
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadMock
import no.nav.helse.bakrommet.sykepengesoknad.enSøknad
import no.nav.helse.bakrommet.taTilBesluting
import no.nav.helse.bakrommet.testutils.`should equal`
import no.nav.helse.bakrommet.util.asJsonNode
import no.nav.helse.bakrommet.util.objectMapper
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
            daoer.outboxDao.hentAlleUpubliserteEntries().size `should equal` 0

            val tokenBeslutter = oAuthMock.token(navIdent = "B111111", grupper = listOf("GRUPPE_BESLUTTER"))

            // Opprett saksbehandlingsperiode
            val periode = opprettSaksbehandlingsperiode()
            daoer.outboxDao.hentAlleUpubliserteEntries().size `should equal` 1
            // Opprett yrkesaktivitet som ordinær arbeidstaker
            val yrkesaktivitetId = opprettYrkesaktivitet(periode.id)

            // Sett sykepengegrunnlag (dette trigger automatisk utbetalingsberegning)
            settSykepengegrunnlag(periode.id, yrkesaktivitetId)

            // Sett dagoversikt med forskjellige dagtyper (dette trigger også utbetalingsberegning)
            settDagoversikt(periode.id, yrkesaktivitetId)

            // Hent utbetalingsberegning
            val beregning = hentUtbetalingsberegning(periode.id)

            // Verifiser resultatet
            verifiserBeregning(beregning!!)

            sendTilBeslutning(periode)
            taTilBesluting(periode, tokenBeslutter)

            godkjenn(periode, tokenBeslutter)
            val upubliserteEntries = daoer.outboxDao.hentAlleUpubliserteEntries()
            upubliserteEntries.size `should equal` 2

            val kafkaPayload = upubliserteEntries.last().kafkaPayload.tilSaksbehandlingsperiodeKafkaDto()
            // TODO: Kafka mapping av dagoversikt er ikke implementert ennå
            // kafkaPayload.yrkesaktiviteter.single().dagoversikt skal inkludere beregningsdata
            kafkaPayload.yrkesaktiviteter.single().dagoversikt `should equal` emptyList()
        }
    }

    private fun String.tilSaksbehandlingsperiodeKafkaDto(): SaksbehandlingsperiodeKafkaDto {
        return objectMapper.readValue(this)
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
                    "ER_SYKMELDT": "ER_SYKMELDT_JA",
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
                    "avslåttBegrunnelse": [],
                    "kilde": "Saksbehandler"
                },
                {
                    "dato": "2024-01-02",
                    "dagtype": "Syk",
                    "grad": 70,
                    "avslåttBegrunnelse": [],
                    "kilde": "Saksbehandler"
                },
                {
                    "dato": "2024-01-03",
                    "dagtype": "Ferie",
                    "avslåttBegrunnelse": [],
                    "kilde": "Saksbehandler"
                },
                {
                    "dato": "2024-01-04",
                    "dagtype": "Syk",
                    "grad": 100,
                    "avslåttBegrunnelse": [],
                    "kilde": "Saksbehandler"
                },
                {
                    "dato": "2024-01-05",
                    "dagtype": "Arbeidsdag",
                    "avslåttBegrunnelse": [],
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
        if (responseText == "null") return null
        
        // Deserialiser JSON som InnDto (siden vi skal konvertere tilbake til domenemodell)
        val json = objectMapper.readTree(responseText)
        val beregningDataJson = json["beregningData"].toString()
        val beregningData = objectMapper.readValue(beregningDataJson, BeregningDataInnDto::class.java)
        
        return BeregningResponse(
            id = UUID.fromString(json["id"].asText()),
            saksbehandlingsperiodeId = UUID.fromString(json["saksbehandlingsperiodeId"].asText()),
            beregningData = beregningData.tilBeregningData(),
            opprettet = json["opprettet"].asText(),
            opprettetAv = json["opprettetAv"].asText(),
            sistOppdatert = json["sistOppdatert"].asText(),
        )
    }
    
    private fun BeregningDataInnDto.tilBeregningData(): BeregningData {
        return BeregningData(
            yrkesaktiviteter = yrkesaktiviteter.map {
                YrkesaktivitetUtbetalingsberegning(
                    yrkesaktivitetId = it.yrkesaktivitetId,
                    utbetalingstidslinje = no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.gjenopprett(it.utbetalingstidslinje),
                    dekningsgrad = it.dekningsgrad,
                )
            },
        )
    }

    private fun verifiserBeregning(beregning: BeregningResponse) {
        assertEquals(1, beregning.beregningData.yrkesaktiviteter.size)

        val yrkesaktivitet = beregning.beregningData.yrkesaktiviteter.first()
        assertEquals(31, yrkesaktivitet.utbetalingstidslinje.size) // Januar 2024 har 31 dager
        yrkesaktivitet.dekningsgrad!!.verdi.prosentDesimal `should equal` 1.0

        // Dag 1: 100% syk - skal ha refusjon
        val dag1 = yrkesaktivitet.utbetalingstidslinje.find { it.dato == LocalDate.of(2024, 1, 1) }!!
        assertEquals(
            184700,
            (dag1.økonomi.personbeløp?.dagligInt ?: 0) * 100,
            "Dag 1 skal ha personutbetaling siden refusjon ikke dekker alt",
        )
        assertEquals(
            46100,
            (dag1.økonomi.arbeidsgiverbeløp?.dagligInt ?: 0) * 100,
            "Dag 1 skal ha 46100 øre refusjon (10000 kr * 12 / 260 dager)",
        )
        assertEquals(100, dag1.økonomi.brukTotalGrad { it }, "Dag 1 skal ha 100% total grad")

        // Dag 2: 70% syk - skal ha 70% refusjon
        val dag2 = yrkesaktivitet.utbetalingstidslinje.find { it.dato == LocalDate.of(2024, 1, 2) }!!
        assertEquals(
            129300,
            (dag2.økonomi.personbeløp?.dagligInt ?: 0) * 100,
            "Dag 2 skal ha personutbetaling siden refusjon ikke dekker alt",
        )
        assertEquals(
            32300,
            (dag2.økonomi.arbeidsgiverbeløp?.dagligInt ?: 0) * 100,
            "Dag 2 skal ha 32300 øre refusjon (70% av dag 1, avrundet)",
        )
        assertEquals(70, dag2.økonomi.brukTotalGrad { it }, "Dag 2 skal ha 70% total grad")

        // Dag 3: Ferie - skal ikke ha utbetaling
        val dag3 = yrkesaktivitet.utbetalingstidslinje.find { it.dato == LocalDate.of(2024, 1, 3) }!!
        assertEquals(0, (dag3.økonomi.personbeløp?.dagligInt ?: 0) * 100, "Dag 3 (Ferie) skal ikke ha utbetaling")
        assertEquals(0, (dag3.økonomi.arbeidsgiverbeløp?.dagligInt ?: 0) * 100, "Dag 3 (Ferie) skal ikke ha refusjon")
        assertEquals(0, dag3.økonomi.brukTotalGrad { it }, "Dag 3 (Ferie) skal ha 0% total grad")

        // Dag 4: 100% syk - skal ha samme refusjon som dag 1
        val dag4 = yrkesaktivitet.utbetalingstidslinje.find { it.dato == LocalDate.of(2024, 1, 4) }!!
        assertEquals(184700, (dag4.økonomi.personbeløp?.dagligInt ?: 0) * 100, "Dag 4 skal ha samme personutbetaling som dag 1")
        assertEquals(46100, (dag4.økonomi.arbeidsgiverbeløp?.dagligInt ?: 0) * 100, "Dag 4 skal ha samme refusjon som dag 1")
        assertEquals(100, dag4.økonomi.brukTotalGrad { it }, "Dag 4 skal ha 100% total grad")

        // Dag 5: Arbeidsdag - skal ikke ha utbetaling
        val dag5 = yrkesaktivitet.utbetalingstidslinje.find { it.dato == LocalDate.of(2024, 1, 5) }!!
        assertEquals(0, (dag5.økonomi.personbeløp?.dagligInt ?: 0) * 100, "Dag 5 (Arbeidsdag) skal ikke ha utbetaling")
        assertEquals(0, (dag5.økonomi.arbeidsgiverbeløp?.dagligInt ?: 0) * 100, "Dag 5 (Arbeidsdag) skal inte ha refusjon")
        assertEquals(0, dag5.økonomi.brukTotalGrad { it }, "Dag 5 (Arbeidsdag) skal ha 0% total grad")
    }
}
