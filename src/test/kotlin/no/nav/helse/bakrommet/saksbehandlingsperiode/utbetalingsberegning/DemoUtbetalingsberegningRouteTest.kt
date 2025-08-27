package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import com.fasterxml.jackson.databind.node.ObjectNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.helse.bakrommet.runApplicationTest
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Inntekt
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Inntektskilde
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Refusjonsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagResponse
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Yrkesaktivitet
import no.nav.helse.bakrommet.util.objectMapper
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
                        Inntekt(
                            yrkesaktivitetId = yrkesaktivitetId,
                            beløpPerMånedØre = 30000L,
                            kilde = Inntektskilde.AINNTEKT,
                            refusjon =
                                listOf(
                                    Refusjonsperiode(
                                        fom = LocalDate.of(2024, 1, 1),
                                        tom = LocalDate.of(2024, 1, 31),
                                        beløpØre = 100L,
                                    ),
                                ),
                        ),
                    ),
                totalInntektØre = 30000L,
                grunnbeløpØre = 100000L,
                grunnbeløp6GØre = 600000L,
                begrensetTil6G = false,
                sykepengegrunnlagØre = 500000L,
                grunnbeløpVirkningstidspunkt = LocalDate.of(2024, 1, 1),
                opprettet = "2024-01-01T10:00:00Z",
                opprettetAv = "test",
                sistOppdatert = "2024-01-01T10:00:00Z",
            )

        // Opprett dagoversikt som JsonNode
        val dagoversikt =
            objectMapper.createArrayNode().apply {
                add(
                    objectMapper.createObjectNode().apply {
                        put("dato", "2024-01-01")
                        put("dagtype", "Syk")
                        put("grad", 100)
                        set<ObjectNode>("avvistBegrunnelse", objectMapper.createArrayNode())
                        put("kilde", "Søknad")
                    },
                )
                add(
                    objectMapper.createObjectNode().apply {
                        put("dato", "2024-01-02")
                        put("dagtype", "Syk")
                        put("grad", 100)
                        set<ObjectNode>("avvistBegrunnelse", objectMapper.createArrayNode())
                        put("kilde", "Søknad")
                    },
                )
                add(
                    objectMapper.createObjectNode().apply {
                        put("dato", "2024-01-03")
                        put("dagtype", "Arbeidsdag")
                        set<ObjectNode>("avvistBegrunnelse", objectMapper.createArrayNode())
                        put("kilde", "Søknad")
                    },
                )
            }

        val yrkesaktivitet =
            Yrkesaktivitet(
                id = yrkesaktivitetId,
                kategorisering = objectMapper.createObjectNode(),
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
                Saksbehandlingsperiode(
                    fom = LocalDate.of(2024, 1, 1),
                    tom = LocalDate.of(2024, 1, 31),
                ),
        )
    }
}
