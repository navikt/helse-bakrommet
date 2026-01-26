package no.nav.helse.bakrommet.e2e.behandling.utbetalingsberegning

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.helse.bakrommet.api.dto.behandling.BehandlingDto
import no.nav.helse.bakrommet.api.dto.utbetalingsberegning.BeregningResponseDto
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.*
import no.nav.helse.bakrommet.asJsonNode
import no.nav.helse.bakrommet.domain.person.NaturligIdent
import no.nav.helse.bakrommet.e2e.TestOppsett
import no.nav.helse.bakrommet.e2e.TestOppsett.oAuthMock
import no.nav.helse.bakrommet.e2e.runApplicationTest
import no.nav.helse.bakrommet.e2e.sykepengesoknad.Arbeidsgiverinfo
import no.nav.helse.bakrommet.e2e.sykepengesoknad.enSøknad
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.godkjennOld
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.hentUtbetalingsberegning
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.opprettYrkesaktivitetOld
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.sendTilBeslutningOld
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.taTilBeslutningOld
import no.nav.helse.bakrommet.e2e.testutils.`should equal`
import no.nav.helse.bakrommet.kafka.dto.saksbehandlingsperiode.SaksbehandlingsperiodeKafkaDto
import no.nav.helse.bakrommet.kafka.dto.saksbehandlingsperiode.SaksbehandlingsperiodeStatusKafkaDto
import no.nav.helse.bakrommet.objectMapper
import no.nav.helse.bakrommet.serialisertTilString
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadMock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class UtbetalingsberegningIntegrasjonTest {
    private companion object {
        const val FNR = "01019012349"
        const val PERSON_ID = "65hth"
        val PERSON_PSEUDO_ID = UUID.nameUUIDFromBytes(PERSON_ID.toByteArray())
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
            sykepengesøknadProvider =
                SykepengesoknadMock.sykepengersoknadBackendClientMock(
                    tokenUtvekslingProvider = TestOppsett.oboClient,
                    søknadIdTilSvar = mapOf(søknad["id"].asText() to søknad),
                ),
        ) { daoer ->
            daoer.personPseudoIdDao.opprettPseudoId(PERSON_PSEUDO_ID, NaturligIdent(FNR))
            daoer.outboxDao.hentAlleUpubliserteEntries().size `should equal` 0

            val tokenBeslutter = oAuthMock.token(navIdent = "B111111", grupper = listOf("GRUPPE_BESLUTTER"))

            // Opprett saksbehandlingsperiode
            val periode = opprettSaksbehandlingsperiode()
            daoer.outboxDao.hentAlleUpubliserteEntries().size `should equal` 1
            // Opprett yrkesaktivitet som ordinær arbeidstaker
            val yrkesaktivitetId =
                opprettYrkesaktivitetOld(
                    personId = PERSON_PSEUDO_ID,
                    periode.id,
                    YrkesaktivitetKategoriseringDto.Arbeidstaker(
                        sykmeldt = true,
                        typeArbeidstaker = TypeArbeidstakerDto.Ordinær(orgnummer = "123456789"),
                    ),
                )
            // Sett inntekt på yrkesaktivitet (dette trigger automatisk utbetalingsberegning)
            settInntektPåYrkesaktivitet(periode.id, yrkesaktivitetId)

            // Sett dagoversikt med forskjellige dagtyper (dette trigger også utbetalingsberegning)
            settDagoversikt(periode.id, yrkesaktivitetId)

            // Hent utbetalingsberegning
            val beregning = hentUtbetalingsberegning(PERSON_PSEUDO_ID, periode.id)

            // Verifiser resultatet
            verifiserBeregning(beregning!!)

            sendTilBeslutningOld(PERSON_PSEUDO_ID, periode.id)
            taTilBeslutningOld(PERSON_PSEUDO_ID, periode.id, tokenBeslutter)

            godkjennOld(PERSON_PSEUDO_ID, periode.id, tokenBeslutter)
            val upubliserteEntries = daoer.outboxDao.hentAlleUpubliserteEntries()
            upubliserteEntries.size `should equal` 4

            val kafkaPayload = upubliserteEntries[1].kafkaPayload.tilSaksbehandlingsperiodeKafkaDto()
            kafkaPayload.status `should equal` SaksbehandlingsperiodeStatusKafkaDto.GODKJENT
            // TODO: Kafka mapping av dagoversikt er ikke implementert ennå
            // kafkaPayload.yrkesaktiviteter.single().dagoversikt skal inkludere beregningsdata
            //  kafkaPayload.yrkesaktiviteter.single().dagoversikt `should equal` emptyList()
        }
    }

    private fun String.tilSaksbehandlingsperiodeKafkaDto(): SaksbehandlingsperiodeKafkaDto = objectMapper.readValue(this)

    private suspend fun ApplicationTestBuilder.opprettSaksbehandlingsperiode(): BehandlingDto {
        client.post("/v1/${PERSON_PSEUDO_ID}/behandlinger") {
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
            client.get("/v1/${PERSON_PSEUDO_ID}/behandlinger") {
                bearerAuth(TestOppsett.userToken)
            }
        assertEquals(200, response.status.value)
        return response.body<List<BehandlingDto>>().first()
    }

    private suspend fun ApplicationTestBuilder.settInntektPåYrkesaktivitet(
        periodeId: UUID,
        yrkesaktivitetId: UUID,
    ) {
        // Månedsinntekt på 50 000 kr (5 000 000 øre) med skjønnsfastsettelse
        val inntektRequest =
            InntektRequestDto.Arbeidstaker(
                ArbeidstakerInntektRequestDto.Skjønnsfastsatt(
                    årsinntekt = 50000.0 * 12,
                    årsak = ArbeidstakerSkjønnsfastsettelseÅrsakDto.MANGELFULL_RAPPORTERING,
                    begrunnelse = "Test skjønnsfastsettelse",
                    refusjon =
                        listOf(
                            RefusjonsperiodeDto(
                                fom = LocalDate.of(2024, 1, 1),
                                tom = LocalDate.of(2024, 1, 31),
                                beløp = 10000.0,
                            ),
                        ),
                ),
            )

        val response =
            client.put("/v1/${PERSON_PSEUDO_ID}/behandlinger/$periodeId/yrkesaktivitet/$yrkesaktivitetId/inntekt") {
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
            {
            "dager": [
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
            }
            """.trimIndent()

        val response =
            client.put("/v1/${PERSON_PSEUDO_ID}/behandlinger/$periodeId/yrkesaktivitet/$yrkesaktivitetId/dagoversikt") {
                bearerAuth(TestOppsett.userToken)
                contentType(ContentType.Application.Json)
                setBody(dagoversikt)
            }
        assertEquals(204, response.status.value)
    }

    private fun verifiserBeregning(beregning: BeregningResponseDto) {
        assertEquals(1, beregning.beregningData.yrkesaktiviteter.size)

        val yrkesaktivitet = beregning.beregningData.yrkesaktiviteter.first()
        assertEquals(31, yrkesaktivitet.utbetalingstidslinje.dager.size) // Januar 2024 har 31 dager
        yrkesaktivitet.dekningsgrad!!.verdi.prosentDesimal `should equal` 1.0

        // Dag 1: 100% syk - skal ha refusjon
        val dag1 = yrkesaktivitet.utbetalingstidslinje.dager.find { it.dato == LocalDate.of(2024, 1, 1) }!!
        assertEquals(
            1846.0,
            dag1.økonomi.personbeløp,
            "Dag 1 skal ha personutbetaling siden refusjon ikke dekker alt",
        )
        assertEquals(
            462.0,
            dag1.økonomi.arbeidsgiverbeløp,
            "Dag 1 skal ha 462 i refusjon",
        )
        assertEquals(1.0, dag1.økonomi.totalGrad, "Dag 1 skal ha 100% total grad")

        // Dag 2: 70% syk - skal ha 70% refusjon
        val dag2 = yrkesaktivitet.utbetalingstidslinje.dager.find { it.dato == LocalDate.of(2024, 1, 2) }!!
        assertEquals(
            1292.0,
            dag2.økonomi.personbeløp,
            "Dag 2 skal ha personutbetaling siden refusjon ikke dekker alt",
        )
        assertEquals(
            323.0,
            dag2.økonomi.arbeidsgiverbeløp,
            "Dag 2 skal ha 323 kr refusjon (70% av dag 1, avrundet)",
        )
        assertEquals(0.7, dag2.økonomi.totalGrad, "Dag 2 skal ha 70% total grad")

        // Dag 3: Ferie - skal ikke ha utbetaling
        val dag3 = yrkesaktivitet.utbetalingstidslinje.dager.find { it.dato == LocalDate.of(2024, 1, 3) }!!
        assertEquals(0.0, (dag3.økonomi.personbeløp ?: 0.0) * 100, "Dag 3 (Ferie) skal ikke ha utbetaling")
        assertEquals(0.0, (dag3.økonomi.arbeidsgiverbeløp ?: 0.0) * 100, "Dag 3 (Ferie) skal ikke ha refusjon")
        assertEquals(0.0, dag3.økonomi.totalGrad, "Dag 3 (Ferie) skal ha 0% total grad")

        // Dag 4: 100% syk - skal ha samme refusjon som dag 1
        val dag4 = yrkesaktivitet.utbetalingstidslinje.dager.find { it.dato == LocalDate.of(2024, 1, 4) }!!
        assertEquals(
            1846.0,
            dag4.økonomi.personbeløp,
            "Dag 4 skal ha samme personutbetaling som dag 1",
        )
        assertEquals(
            462.0,
            dag4.økonomi.arbeidsgiverbeløp,
            "Dag 4 skal ha samme refusjon som dag 1",
        )
        assertEquals(1.0, dag4.økonomi.totalGrad, "Dag 4 skal ha 100% total grad")

        // Dag 5: Arbeidsdag - skal ikke ha utbetaling
        val dag5 = yrkesaktivitet.utbetalingstidslinje.dager.find { it.dato == LocalDate.of(2024, 1, 5) }!!
        assertEquals(0.0, (dag5.økonomi.personbeløp ?: 0.0) * 100, "Dag 5 (Arbeidsdag) skal ikke ha utbetaling")
        assertEquals(
            0.0,
            dag5.økonomi.arbeidsgiverbeløp,
            "Dag 5 (Arbeidsdag) skal ikke ha refusjon",
        )
        assertEquals(0.0, dag5.økonomi.totalGrad, "Dag 5 (Arbeidsdag) skal ha 0% total grad")
    }
}
