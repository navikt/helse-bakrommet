package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.helse.bakrommet.runApplicationTest
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dag
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dagtype
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Kilde
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.InntektBeregnet
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Inntektskilde
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagResponse
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Yrkesaktivitet
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
            assertTrue(responseBody.contains("error"))
        }

    private fun lagTestInput(yrkesaktivitetId: UUID): UtbetalingsberegningInput {
        val sykepengegrunnlag =
            SykepengegrunnlagResponse(
                id = UUID.randomUUID(),
                saksbehandlingsperiodeId = UUID.randomUUID(),
                inntekter =
                    listOf(
                        InntektBeregnet(
                            yrkesaktivitetId = yrkesaktivitetId,
                            inntektMånedligØre = 500000L,
                            grunnlagMånedligØre = 500000L,
                            kilde = Inntektskilde.AINNTEKT,
                            refusjon =
                                listOf(),
                        ),
                    ),
                totalInntektØre = 500000L,
                grunnbeløpØre = 100000L,
                grunnbeløp6GØre = 600000L,
                begrensetTil6G = false,
                sykepengegrunnlagØre = 500000L,
                grunnbeløpVirkningstidspunkt = LocalDate.of(2024, 1, 1),
                opprettet = "2024-01-01T10:00:00Z",
                opprettetAv = "test",
                sistOppdatert = "2024-01-01T10:00:00Z",
            )

        // Opprett dagoversikt som List<Dag>
        val dagoversikt =
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
            )

        val yrkesaktivitet =
            Yrkesaktivitet(
                id = yrkesaktivitetId,
                kategorisering =
                    HashMap<String, String>().apply {
                        put("INNTEKTSKATEGORI", "ARBEIDSTAKER")
                        put("ORGNUMMER", "123456789")
                        put("ER_SYKMELDT", "ER_SYKMELDT_JA")
                        put("TYPE_ARBEIDSTAKER", "ORDINÆRT_ARBEIDSFORHOLD")
                    },
                kategoriseringGenerert = null,
                dagoversikt = dagoversikt,
                dagoversiktGenerert = null,
                saksbehandlingsperiodeId = UUID.randomUUID(),
                opprettet = OffsetDateTime.now(),
                generertFraDokumenter = emptyList(),
            )

        return UtbetalingsberegningInput(
            sykepengegrunnlag = sykepengegrunnlag,
            yrkesaktivitet = listOf(yrkesaktivitet),
            saksbehandlingsperiode =
                PeriodeDto(
                    fom = LocalDate.of(2024, 1, 1),
                    tom = LocalDate.of(2024, 1, 31),
                ),
        )
    }
}
