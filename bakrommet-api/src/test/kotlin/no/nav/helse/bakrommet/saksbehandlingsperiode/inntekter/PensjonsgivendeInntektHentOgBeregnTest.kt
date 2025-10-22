package no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter

import no.nav.helse.Grunnbeløp
import no.nav.helse.bakrommet.testutils.`should equal`
import no.nav.helse.dto.InntektbeløpDto
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.Year
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PensjonsgivendeInntektHentOgBeregnTest {
    private val skjæringstidspunkt = LocalDate.of(2024, 6, 1)
    private val anvendtGrunnbeløp = Grunnbeløp.`1G`.beløp(skjæringstidspunkt) // 2024: 124 028 kr

    @Test
    fun `skal kaste feil når det er færre enn 3 år med inntekt`() {
        val responses =
            listOf(
                lagHentPensjonsgivendeInntektResponse("2022", 500000),
                lagHentPensjonsgivendeInntektResponse("2023", 600000),
                // Mangler 2021
            )

        val exception =
            assertThrows<RuntimeException> {
                responses.tilBeregnetPensjonsgivendeInntekt(skjæringstidspunkt)
            }

        assertEquals("For få år med pensjonsgivende inntekt fra sigrun. Må skjønnsfastsettes", exception.message)
    }

    @Test
    fun `skal beregne korrekt for inntekter under 6G`() {
        // Alle inntekter under 6G (2021-2023)
        val responses =
            listOf(
                lagHentPensjonsgivendeInntektResponse("2021", 300000), // ~3G
                lagHentPensjonsgivendeInntektResponse("2022", 400000), // ~3,6G
                lagHentPensjonsgivendeInntektResponse("2023", 500000), // ~4,2G
            )

        val result = responses.tilBeregnetPensjonsgivendeInntekt(skjæringstidspunkt)

        result.omregnetÅrsinntekt.beløp `should equal` 446909.0
        result.pensjonsgivendeInntekt `should equal`
            listOf(
                InntektAar(
                    år = Year.of(2021),
                    antallGKompensert = 2.864891707093472,
                    justertÅrsgrunnlag = InntektbeløpDto.Årlig(355326.7886473891),
                    rapportertinntekt = InntektbeløpDto.Årlig(300000.0),
                    snittG = InntektbeløpDto.Årlig(104716.0),
                ),
                InntektAar(
                    år = Year.of(2022),
                    antallGKompensert = 3.643518181155724,
                    justertÅrsgrunnlag = InntektbeløpDto.Årlig(451898.27297238214),
                    rapportertinntekt = InntektbeløpDto.Årlig(400000.0),
                    snittG = InntektbeløpDto.Årlig(109784.0),
                ),
                InntektAar(
                    år = Year.of(2023),
                    antallGKompensert = 4.301482290797408,
                    justertÅrsgrunnlag = InntektbeløpDto.Årlig(533504.2455630209),
                    rapportertinntekt = InntektbeløpDto.Årlig(500000.0),
                    snittG = InntektbeløpDto.Årlig(116239.0),
                ),
            )
    }

    @Test
    fun `skal beregne korrekt for inntekter mellom 6G og 12G`() {
        // Alle inntekter mellom 6G og 12G
        val responses =
            listOf(
                lagHentPensjonsgivendeInntektResponse("2021", 800000),
                lagHentPensjonsgivendeInntektResponse("2022", 900000),
                lagHentPensjonsgivendeInntektResponse("2023", 1000000),
            )

        val result = responses.tilBeregnetPensjonsgivendeInntekt(skjæringstidspunkt)
        result.omregnetÅrsinntekt.beløp `should equal` 832925.0
        result.pensjonsgivendeInntekt[0] `should equal`
            InntektAar(
                år = Year.of(2021),
                antallGKompensert = 6.5465704063053085,
                justertÅrsgrunnlag = InntektbeløpDto.Årlig(811958.0343532349),
                rapportertinntekt = InntektbeløpDto.Årlig(800000.0),
                snittG = InntektbeløpDto.Årlig(104716.0),
            )
        result.pensjonsgivendeInntekt[1] `should equal`
            InntektAar(
                år = Year.of(2022),
                antallGKompensert = 6.732638635866793,
                justertÅrsgrunnlag = InntektbeløpDto.Årlig(835035.7047292867),
                rapportertinntekt = InntektbeløpDto.Årlig(900000.0),
                snittG = InntektbeløpDto.Årlig(109784.0),
            )
        result.pensjonsgivendeInntekt[2] `should equal`
            InntektAar(
                år = Year.of(2023),
                antallGKompensert = 6.867654860531606,
                justertÅrsgrunnlag = InntektbeløpDto.Årlig(851781.497042014),
                rapportertinntekt = InntektbeløpDto.Årlig(1000000.0),
                snittG = InntektbeløpDto.Årlig(116239.0),
            )
    }

    @Test
    fun `skal beregne korrekt for inntekter over 12G`() {
        // Alle inntekter over 12G
        val responses =
            listOf(
                lagHentPensjonsgivendeInntektResponse("2021", 1500000), // ~13,5G
                lagHentPensjonsgivendeInntektResponse("2022", 1600000), // ~14,4G
                lagHentPensjonsgivendeInntektResponse("2023", 1700000), // ~14,3G
            )

        val result = responses.tilBeregnetPensjonsgivendeInntekt(skjæringstidspunkt)

        // Verifiser at alle inntekter er 8G kompensert
        result.pensjonsgivendeInntekt.forEach { inntekt ->
            assertTrue(
                inntekt.antallGKompensert == 8.0,
                "Inntekt for ${inntekt.år} skal være 8G, men var ${inntekt.antallGKompensert}",
            )
        }

        // Verifiser at maksimalt 6G + 6G*1/3 = 8G kompenseres
        result.pensjonsgivendeInntekt.forEach { inntekt ->
            val maksimalKompensertG = 6.0 + 6.0 * (1.0 / 3.0) // 8G
            val faktiskKompensertG = inntekt.justertÅrsgrunnlag.beløp / anvendtGrunnbeløp.årlig
            assertEquals(
                maksimalKompensertG,
                faktiskKompensertG,
                0.1,
                "Kompensert G for ${inntekt.år} skal være maksimalt $maksimalKompensertG",
            )
        }

        result.omregnetÅrsinntekt.beløp `should equal` 992224.0
        result.pensjonsgivendeInntekt `should equal`
            listOf(
                InntektAar(
                    år = Year.of(2021),
                    antallGKompensert = 8.0,
                    justertÅrsgrunnlag = InntektbeløpDto.Årlig(992224.0),
                    rapportertinntekt = InntektbeløpDto.Årlig(1500000.0),
                    snittG = InntektbeløpDto.Årlig(104716.0),
                ),
                InntektAar(
                    år = Year.of(2022),
                    antallGKompensert = 8.0,
                    justertÅrsgrunnlag = InntektbeløpDto.Årlig(992224.0),
                    rapportertinntekt = InntektbeløpDto.Årlig(1600000.0),
                    snittG = InntektbeløpDto.Årlig(109784.0),
                ),
                InntektAar(
                    år = Year.of(2023),
                    antallGKompensert = 8.0,
                    justertÅrsgrunnlag = InntektbeløpDto.Årlig(992224.0),
                    rapportertinntekt = InntektbeløpDto.Årlig(1700000.0),
                    snittG = InntektbeløpDto.Årlig(116239.0),
                ),
            )
    }

    @Test
    fun `skal beregne korrekt for blandede G-nivåer`() {
        // Blandet scenario: under 6G, mellom 6G-12G, og over 12G
        val responses =
            listOf(
                lagHentPensjonsgivendeInntektResponse("2021", 400000), // ~3,6G (under 6G)
                lagHentPensjonsgivendeInntektResponse("2022", 800000), // ~7,2G (mellom 6G-12G)
                lagHentPensjonsgivendeInntektResponse("2023", 1500000), // ~13,5G (over 12G)
            )

        val result = responses.tilBeregnetPensjonsgivendeInntekt(skjæringstidspunkt)

        result.omregnetÅrsinntekt.beløp `should equal` 754456.0
        result.pensjonsgivendeInntekt[0] `should equal`
            InntektAar(
                år = Year.of(2021),
                antallGKompensert = 3.8198556094579623,
                justertÅrsgrunnlag = InntektbeløpDto.Årlig(473769.0515298521),
                rapportertinntekt = InntektbeløpDto.Årlig(400000.0),
                snittG = InntektbeløpDto.Årlig(104716.0),
            )
        result.pensjonsgivendeInntekt[1] `should equal`
            InntektAar(
                år = Year.of(2022),
                antallGKompensert = 6.429012120770483,
                justertÅrsgrunnlag = InntektbeløpDto.Årlig(797377.5153149214),
                rapportertinntekt = InntektbeløpDto.Årlig(800000.0),
                snittG = InntektbeløpDto.Årlig(109784.0),
            )
        result.pensjonsgivendeInntekt[2] `should equal`
            InntektAar(
                år = Year.of(2023),
                antallGKompensert = 8.0,
                justertÅrsgrunnlag = InntektbeløpDto.Årlig(992224.0),
                rapportertinntekt = InntektbeløpDto.Årlig(1500000.0),
                snittG = InntektbeløpDto.Årlig(116239.0),
            )
    }

    @Test
    fun `skal håndtere null pensjonsgivende inntekt`() {
        val responses =
            listOf(
                lagHentPensjonsgivendeInntektResponse("2021", 400000),
                lagHentPensjonsgivendeInntektResponse("2022", 600000),
                HentPensjonsgivendeInntektResponse("12345678901", "2023", null), // null inntekt
            )

        val exception =
            assertThrows<RuntimeException> {
                responses.tilBeregnetPensjonsgivendeInntekt(skjæringstidspunkt)
            }

        assertEquals("For få år med pensjonsgivende inntekt fra sigrun. Må skjønnsfastsettes", exception.message)
    }

    private fun lagHentPensjonsgivendeInntektResponse(
        år: String,
        inntekt: Int,
    ): HentPensjonsgivendeInntektResponse =
        HentPensjonsgivendeInntektResponse(
            norskPersonidentifikator = "12345678901",
            inntektsaar = år,
            pensjonsgivendeInntekt =
                listOf(
                    PensjonsgivendeInntekt(
                        datoForFastsetting = "2023-05-01",
                        skatteordning = Skatteordning.FASTLAND,
                        pensjonsgivendeInntektAvNaeringsinntekt = inntekt,
                        pensjonsgivendeInntektAvLoennsinntekt = 0,
                        pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel = 0,
                        pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage = 0,
                    ),
                ),
        )
}
