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
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.ArbeidstakerInntektRequest
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.ArbeidstakerSkjønnsfastsettelseÅrsak
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.InntektRequest
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Refusjonsperiode
import no.nav.helse.bakrommet.sendTilBeslutning
import no.nav.helse.bakrommet.sykepengesoknad.Arbeidsgiverinfo
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadMock
import no.nav.helse.bakrommet.sykepengesoknad.enSøknad
import no.nav.helse.bakrommet.taTilBesluting
import no.nav.helse.bakrommet.testutils.`should equal`
import no.nav.helse.bakrommet.util.asJsonNode
import no.nav.helse.bakrommet.util.objectMapper
import no.nav.helse.bakrommet.util.serialisertTilString
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.dto.serialisering.UtbetalingsdagUtDto
import no.nav.helse.dto.serialisering.UtbetalingstidslinjeUtDto
import no.nav.helse.dto.serialisering.ØkonomiUtDto
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

            // Sett inntekt på yrkesaktivitet (dette trigger automatisk utbetalingsberegning)
            settInntektPåYrkesaktivitet(periode.id, yrkesaktivitetId)

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

    private fun String.tilSaksbehandlingsperiodeKafkaDto(): SaksbehandlingsperiodeKafkaDto = objectMapper.readValue(this)

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
                    "ORGNUMMER": "$ARBEIDSGIVER_ORGNR",
                    "ER_SYKMELDT": "ER_SYKMELDT_JA",
                    "TYPE_ARBEIDSTAKER": "ORDINÆRT_ARBEIDSFORHOLD"
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

    private suspend fun ApplicationTestBuilder.settInntektPåYrkesaktivitet(
        periodeId: UUID,
        yrkesaktivitetId: UUID,
    ) {
        // Månedsinntekt på 50 000 kr (5 000 000 øre) med skjønnsfastsettelse
        val inntektRequest =
            InntektRequest.Arbeidstaker(
                ArbeidstakerInntektRequest.Skjønnsfastsatt(
                    årsinntekt = InntektbeløpDto.Årlig(50000.0 * 12),
                    årsak = ArbeidstakerSkjønnsfastsettelseÅrsak.MANGELFULL_RAPPORTERING,
                    begrunnelse = "Test skjønnsfastsettelse",
                    refusjon =
                        listOf(
                            Refusjonsperiode(
                                fom = LocalDate.of(2024, 1, 1),
                                tom = LocalDate.of(2024, 1, 31),
                                beløp = InntektbeløpDto.MånedligDouble(10000.0),
                            ),
                        ),
                ),
            )

        val response =
            client.put("/v1/$PERSON_ID/saksbehandlingsperioder/$periodeId/yrkesaktivitet/$yrkesaktivitetId/inntekt") {
                bearerAuth(TestOppsett.userToken)
                contentType(ContentType.Application.Json)
                setBody(inntektRequest.serialisertTilString())
            }
        assertEquals(204, response.status.value)
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

        // API-et sender BeregningResponseUtDto (med InntektDto som har alle felt)
        val responseUtDto = objectMapper.readValue(responseText, BeregningResponseUtDto::class.java)

        // Konverter til domenemodell
        return BeregningResponse(
            id = responseUtDto.id,
            saksbehandlingsperiodeId = responseUtDto.saksbehandlingsperiodeId,
            beregningData = responseUtDto.beregningData.tilBeregningData(),
            opprettet = responseUtDto.opprettet,
            opprettetAv = responseUtDto.opprettetAv,
            sistOppdatert = responseUtDto.sistOppdatert,
        )
    }

    private fun BeregningDataUtDto.tilBeregningData(): BeregningData {
        // Konverter UtDto til InnDto for å bruke den eksisterende gjenopprett-logikken
        val innDto = this.tilBeregningDataInnDto()
        return BeregningData(
            yrkesaktiviteter =
                innDto.yrkesaktiviteter.map {
                    YrkesaktivitetUtbetalingsberegning(
                        yrkesaktivitetId = it.yrkesaktivitetId,
                        utbetalingstidslinje =
                            no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
                                .gjenopprett(it.utbetalingstidslinje),
                        dekningsgrad = it.dekningsgrad,
                    )
                },
        )
    }

    private fun BeregningDataUtDto.tilBeregningDataInnDto(): BeregningDataInnDto =
        BeregningDataInnDto(
            yrkesaktiviteter = yrkesaktiviteter.map { it.tilInnDto() },
        )

    private fun YrkesaktivitetUtbetalingsberegningUtDto.tilInnDto(): YrkesaktivitetUtbetalingsberegningInnDto =
        YrkesaktivitetUtbetalingsberegningInnDto(
            yrkesaktivitetId = yrkesaktivitetId,
            utbetalingstidslinje = utbetalingstidslinje.tilInnDto(),
            dekningsgrad = dekningsgrad,
        )

    private fun UtbetalingstidslinjeUtDto.tilInnDto(): no.nav.helse.dto.deserialisering.UtbetalingstidslinjeInnDto =
        no.nav.helse.dto.deserialisering.UtbetalingstidslinjeInnDto(
            dager = dager.map { it.tilInnDto() },
        )

    private fun UtbetalingsdagUtDto.tilInnDto(): no.nav.helse.dto.deserialisering.UtbetalingsdagInnDto =
        when (this) {
            is UtbetalingsdagUtDto.NavDagDto ->
                no.nav.helse.dto.deserialisering.UtbetalingsdagInnDto
                    .NavDagDto(dato, økonomi.tilInnDto())

            is UtbetalingsdagUtDto.ArbeidsgiverperiodeDagDto ->
                no.nav.helse.dto.deserialisering.UtbetalingsdagInnDto.ArbeidsgiverperiodeDagDto(
                    dato,
                    økonomi.tilInnDto(),
                )

            is UtbetalingsdagUtDto.ArbeidsgiverperiodeDagNavDto ->
                no.nav.helse.dto.deserialisering.UtbetalingsdagInnDto.ArbeidsgiverperiodeDagNavDto(
                    dato,
                    økonomi.tilInnDto(),
                )

            is UtbetalingsdagUtDto.NavHelgDagDto ->
                no.nav.helse.dto.deserialisering.UtbetalingsdagInnDto.NavHelgDagDto(
                    dato,
                    økonomi.tilInnDto(),
                )

            is UtbetalingsdagUtDto.ArbeidsdagDto ->
                no.nav.helse.dto.deserialisering.UtbetalingsdagInnDto.ArbeidsdagDto(
                    dato,
                    økonomi.tilInnDto(),
                )

            is UtbetalingsdagUtDto.FridagDto ->
                no.nav.helse.dto.deserialisering.UtbetalingsdagInnDto
                    .FridagDto(dato, økonomi.tilInnDto())

            is UtbetalingsdagUtDto.AvvistDagDto ->
                no.nav.helse.dto.deserialisering.UtbetalingsdagInnDto.AvvistDagDto(
                    dato,
                    økonomi.tilInnDto(),
                    begrunnelser,
                )

            is UtbetalingsdagUtDto.ForeldetDagDto ->
                no.nav.helse.dto.deserialisering.UtbetalingsdagInnDto.ForeldetDagDto(
                    dato,
                    økonomi.tilInnDto(),
                )

            is UtbetalingsdagUtDto.UkjentDagDto ->
                no.nav.helse.dto.deserialisering.UtbetalingsdagInnDto.UkjentDagDto(
                    dato,
                    økonomi.tilInnDto(),
                )

            is UtbetalingsdagUtDto.VentetidsdagDto ->
                no.nav.helse.dto.deserialisering.UtbetalingsdagInnDto.VentetidsdagDto(
                    dato,
                    økonomi.tilInnDto(),
                )
        }

    private fun ØkonomiUtDto.tilInnDto(): no.nav.helse.dto.deserialisering.ØkonomiInnDto =
        no.nav.helse.dto.deserialisering.ØkonomiInnDto(
            grad = grad,
            totalGrad = totalGrad,
            utbetalingsgrad = utbetalingsgrad,
            arbeidsgiverRefusjonsbeløp = arbeidsgiverRefusjonsbeløp.dagligDouble,
            aktuellDagsinntekt = aktuellDagsinntekt.dagligDouble,
            inntektjustering = inntektjustering.dagligDouble,
            dekningsgrad = dekningsgrad,
            arbeidsgiverbeløp = arbeidsgiverbeløp?.dagligDouble,
            personbeløp = personbeløp?.dagligDouble,
            reservertArbeidsgiverbeløp = reservertArbeidsgiverbeløp?.dagligDouble,
            reservertPersonbeløp = reservertPersonbeløp?.dagligDouble,
        )

    private fun verifiserBeregning(beregning: BeregningResponse) {
        assertEquals(1, beregning.beregningData.yrkesaktiviteter.size)

        val yrkesaktivitet = beregning.beregningData.yrkesaktiviteter.first()
        assertEquals(31, yrkesaktivitet.utbetalingstidslinje.size) // Januar 2024 har 31 dager
        yrkesaktivitet.dekningsgrad!!.verdi.prosentDesimal `should equal` 1.0

        // Dag 1: 100% syk - skal ha refusjon
        val dag1 = yrkesaktivitet.utbetalingstidslinje.find { it.dato == LocalDate.of(2024, 1, 1) }!!
        assertEquals(
            1846,
            dag1.økonomi.personbeløp?.dagligInt,
            "Dag 1 skal ha personutbetaling siden refusjon ikke dekker alt",
        )
        assertEquals(
            462,
            dag1.økonomi.arbeidsgiverbeløp?.dagligInt,
            "Dag 1 skal ha 462 i refusjon",
        )
        assertEquals(100, dag1.økonomi.brukTotalGrad { it }, "Dag 1 skal ha 100% total grad")

        // Dag 2: 70% syk - skal ha 70% refusjon
        val dag2 = yrkesaktivitet.utbetalingstidslinje.find { it.dato == LocalDate.of(2024, 1, 2) }!!
        assertEquals(
            1292,
            dag2.økonomi.personbeløp?.dagligInt,
            "Dag 2 skal ha personutbetaling siden refusjon ikke dekker alt",
        )
        assertEquals(
            323,
            dag2.økonomi.arbeidsgiverbeløp?.dagligInt,
            "Dag 2 skal ha 323 kr refusjon (70% av dag 1, avrundet)",
        )
        assertEquals(70, dag2.økonomi.brukTotalGrad { it }, "Dag 2 skal ha 70% total grad")

        // Dag 3: Ferie - skal ikke ha utbetaling
        val dag3 = yrkesaktivitet.utbetalingstidslinje.find { it.dato == LocalDate.of(2024, 1, 3) }!!
        assertEquals(0, (dag3.økonomi.personbeløp?.dagligInt ?: 0) * 100, "Dag 3 (Ferie) skal ikke ha utbetaling")
        assertEquals(0, (dag3.økonomi.arbeidsgiverbeløp?.dagligInt ?: 0) * 100, "Dag 3 (Ferie) skal ikke ha refusjon")
        assertEquals(0, dag3.økonomi.brukTotalGrad { it }, "Dag 3 (Ferie) skal ha 0% total grad")

        // Dag 4: 100% syk - skal ha samme refusjon som dag 1
        val dag4 = yrkesaktivitet.utbetalingstidslinje.find { it.dato == LocalDate.of(2024, 1, 4) }!!
        assertEquals(
            1846,
            dag4.økonomi.personbeløp?.dagligInt,
            "Dag 4 skal ha samme personutbetaling som dag 1",
        )
        assertEquals(
            462,
            dag4.økonomi.arbeidsgiverbeløp?.dagligInt,
            "Dag 4 skal ha samme refusjon som dag 1",
        )
        assertEquals(100, dag4.økonomi.brukTotalGrad { it }, "Dag 4 skal ha 100% total grad")

        // Dag 5: Arbeidsdag - skal ikke ha utbetaling
        val dag5 = yrkesaktivitet.utbetalingstidslinje.find { it.dato == LocalDate.of(2024, 1, 5) }!!
        assertEquals(0, (dag5.økonomi.personbeløp?.dagligInt ?: 0) * 100, "Dag 5 (Arbeidsdag) skal ikke ha utbetaling")
        assertEquals(
            0,
            dag5.økonomi.arbeidsgiverbeløp?.dagligInt,
            "Dag 5 (Arbeidsdag) skal ikke ha refusjon",
        )
        assertEquals(0, dag5.økonomi.brukTotalGrad { it }, "Dag 5 (Arbeidsdag) skal ha 0% total grad")
    }
}
