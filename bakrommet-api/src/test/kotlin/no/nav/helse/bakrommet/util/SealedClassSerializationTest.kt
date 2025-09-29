package no.nav.helse.bakrommet.util

import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.BeregningData
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.BeregningDataInnDto
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.BeregningDataUtDto
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.YrkesaktivitetUtbetalingsberegningUtDto
import no.nav.helse.dto.ProsentdelDto
import no.nav.helse.dto.serialisering.UtbetalingsdagUtDto
import no.nav.helse.dto.serialisering.UtbetalingstidslinjeUtDto
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.økonomi.Økonomi
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SealedClassSerializationTest {

    @Test
    fun `serialiserer og deserialiserer BeregningDataUtDto med sealed classes`() {
        // Arrange - lag en utbetalingstidslinje med ulike dagtyper
        val utbetalingstidslinje = Utbetalingstidslinje(
            listOf(
                Utbetalingsdag.NavDag(
                    LocalDate.of(2023, 1, 1),
                    Økonomi.ikkeBetalt(),
                ),
                Utbetalingsdag.Fridag(
                    LocalDate.of(2023, 1, 2),
                    Økonomi.ikkeBetalt(),
                ),
                Utbetalingsdag.ArbeidsgiverperiodeDag(
                    LocalDate.of(2023, 1, 3),
                    Økonomi.ikkeBetalt(),
                ),
            ),
        )

        val beregningData = BeregningData(
            yrkesaktiviteter = listOf(
                no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.YrkesaktivitetUtbetalingsberegning(
                    yrkesaktivitetId = UUID.randomUUID(),
                    utbetalingstidslinje = utbetalingstidslinje,
                    dekningsgrad = no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.Sporbar(
                        verdi = ProsentdelDto(1.0),
                        sporing = no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.Beregningssporing.ARBEIDSTAKER_100,
                    ),
                ),
            ),
        )

        // Convert til UtDto (som skjer ved serialisering)
        val beregningDataUtDto = BeregningDataUtDto(
            yrkesaktiviteter = beregningData.yrkesaktiviteter.map {
                YrkesaktivitetUtbetalingsberegningUtDto(
                    yrkesaktivitetId = it.yrkesaktivitetId,
                    utbetalingstidslinje = it.utbetalingstidslinje.dto(),
                    dekningsgrad = it.dekningsgrad,
                )
            },
        )

        // Act - serialiser til JSON (simulerer lagring i database)
        val json = objectMapper.writeValueAsString(beregningDataUtDto)

        println("=== Serialisert JSON ===")
        println(json)
        println("========================")

        // Assert - sjekk at JSON inneholder @type felt
        assertTrue(json.contains("\"@type\""), "JSON skal inneholde @type felt for sealed classes")
        assertTrue(json.contains("NavDagDto") || json.contains("nav"), "JSON skal inneholde NavDagDto type")
        assertTrue(json.contains("FridagDto") || json.contains("fridag"), "JSON skal inneholde FridagDto type")
        assertTrue(
            json.contains("ArbeidsgiverperiodeDagDto") || json.contains("arbeidsgiverperiode"),
            "JSON skal inneholde ArbeidsgiverperiodeDagDto type",
        )

        // Act - deserialiser fra JSON (simulerer lesing fra database)
        val deserialisertData = objectMapper.readValue(json, BeregningDataInnDto::class.java)

        // Assert - sjekk at vi får tilbake riktig antall dager
        assertEquals(1, deserialisertData.yrkesaktiviteter.size, "Skal ha én yrkesaktivitet")
        assertEquals(3, deserialisertData.yrkesaktiviteter[0].utbetalingstidslinje.dager.size, "Skal ha tre dager")

        // Konverter tilbake til domain model
        val gjenopprettetTidslinje = Utbetalingstidslinje.gjenopprett(
            deserialisertData.yrkesaktiviteter[0].utbetalingstidslinje,
        )

        // Assert - sjekk at vi kan gjenopprette tidslinjen
        assertEquals(3, gjenopprettetTidslinje.size, "Gjenopprettet tidslinje skal ha tre dager")
    }

    @Test
    fun `serialiserer enkelt UtbetalingsdagUtDto med type-informasjon`() {
        // Arrange
        val navDag = UtbetalingsdagUtDto.NavDagDto(
            dato = LocalDate.of(2023, 1, 1),
            økonomi = Økonomi.ikkeBetalt().dto(),
        )

        // Act
        val json = objectMapper.writeValueAsString(navDag)

        println("\n=== Serialisert enkelt NavDagDto ===")
        println(json)
        println("====================================")

        // Assert
        assertTrue(json.contains("\"@type\""), "JSON skal inneholde @type felt")
        assertTrue(json.contains("NavDagDto"), "JSON skal inneholde NavDagDto som type")
    }
}
