package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.helse.bakrommet.runApplicationTest
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dag
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dagtype
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Kilde
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.InntektData
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Sykepengegrunnlag
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.TypeArbeidstaker
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Yrkesaktivitet
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.tilYrkesaktivitetDbRecord
import no.nav.helse.bakrommet.testutils.`should contain`
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.dto.PeriodeDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

class DemoUtbetalingsberegningRouteTest {
    @Test
    fun `demo utbetalingsberegning API er åpent og fungerer korrekt`() =
        runApplicationTest {
            // Opprett test data
            val yrkesaktivitetId = UUID.randomUUID()
            val input = lagTestInput(yrkesaktivitetId)

            // Kall demo API uten autentisering
            val response =
                client.post("/api/demo/utbetalingsberegning") {
                    contentType(ContentType.Application.Json)
                    setBody(input)
                }

            // Verifiser at API-et er åpent (ingen 401/403)
            assertEquals(200, response.status.value)

            // Verifiser respons
            val responseBody = response.bodyAsText()
            assertTrue(responseBody.contains("yrkesaktiviteter"))
            assertTrue(responseBody.contains(yrkesaktivitetId.toString()))
        }

    @Test
    fun `demo utbetalingsberegning API håndterer feil input korrekt`() =
        runApplicationTest {
            // Kall demo API med ugyldig input
            val response =
                client.post("/api/demo/utbetalingsberegning") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"invalid": "data"}""")
                }

            // Verifiser at API-et returnerer 400 Bad Request
            assertEquals(400, response.status.value)

            // Verifiser at feilmelding er inkludert
            val responseBody = response.bodyAsText()
            responseBody `should contain` "Ugyldig forespørsel"
            responseBody `should contain` "due to missing (therefore NULL) value for creator parameter sykepengegrunnlag"
        }

    private fun lagTestInput(yrkesaktivitetId: UUID): DemoUtbetalingsberegningInput {
        // Opprett sykepengegrunnlag med ny struktur
        val sykepengegrunnlag =
            Sykepengegrunnlag(
                grunnbeløp = InntektbeløpDto.Årlig(100000.0),
                totaltInntektsgrunnlag = InntektbeløpDto.Årlig(500000.0),
                sykepengegrunnlag = InntektbeløpDto.Årlig(500000.0),
                seksG = InntektbeløpDto.Årlig(600000.0),
                begrensetTil6G = false,
                grunnbeløpVirkningstidspunkt = LocalDate.of(2024, 1, 1),
                næringsdel = null,
            )

        // Opprett yrkesaktivitet med ny struktur
        val yrkesaktivitet =
            Yrkesaktivitet(
                id = yrkesaktivitetId,
                kategorisering =
                    YrkesaktivitetKategorisering.Arbeidstaker(
                        sykmeldt = true,
                        orgnummer = "123456789",
                        typeArbeidstaker = TypeArbeidstaker.ORDINÆRT_ARBEIDSFORHOLD,
                    ),
                kategoriseringGenerert = null,
                dagoversikt =
                    listOf(
                        Dag(
                            dato = LocalDate.of(2024, 1, 1),
                            dagtype = Dagtype.Syk,
                            grad = 100,
                            avslåttBegrunnelse = emptyList(),
                            andreYtelserBegrunnelse = null,
                            kilde = Kilde.Søknad,
                        ),
                        Dag(
                            dato = LocalDate.of(2024, 1, 2),
                            dagtype = Dagtype.Syk,
                            grad = 100,
                            avslåttBegrunnelse = emptyList(),
                            andreYtelserBegrunnelse = null,
                            kilde = Kilde.Søknad,
                        ),
                        Dag(
                            dato = LocalDate.of(2024, 1, 3),
                            dagtype = Dagtype.Arbeidsdag,
                            grad = null,
                            avslåttBegrunnelse = emptyList(),
                            andreYtelserBegrunnelse = null,
                            kilde = Kilde.Søknad,
                        ),
                    ),
                dagoversiktGenerert = null,
                saksbehandlingsperiodeId = UUID.randomUUID(),
                opprettet = OffsetDateTime.now(),
                generertFraDokumenter = emptyList(),
                perioder = null,
                inntektRequest = null,
                inntektData =
                    InntektData.ArbeidstakerManueltBeregnet(
                        omregnetÅrsinntekt = InntektbeløpDto.Årlig(500000.0),
                    ),
                refusjonsdata = null,
            )

        return DemoUtbetalingsberegningInput(
            sykepengegrunnlag = sykepengegrunnlag,
            yrkesaktivitet = listOf(yrkesaktivitet.tilYrkesaktivitetDbRecord()),
            saksbehandlingsperiode =
                PeriodeDto(
                    fom = LocalDate.of(2024, 1, 1),
                    tom = LocalDate.of(2024, 1, 31),
                ),
        )
    }
}
