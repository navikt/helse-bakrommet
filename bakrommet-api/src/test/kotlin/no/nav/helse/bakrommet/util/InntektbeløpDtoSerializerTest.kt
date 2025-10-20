package no.nav.helse.bakrommet.util

import no.nav.helse.bakrommet.serde.objectMapperCustomSerde
import no.nav.helse.dto.InntektbeløpDto
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class InntektbeløpDtoSerializerTest {
    @Test
    fun `serialiserer InntektbeløpDto Årlig som bare tall`() {
        val årlig = InntektbeløpDto.Årlig(130160.0)
        val json = objectMapperCustomSerde.writeValueAsString(årlig)

        assertEquals("130160.0", json)
    }

    @Test
    fun `serialiserer InntektbeløpDto MånedligDouble som bare tall`() {
        val månedlig = InntektbeløpDto.MånedligDouble(400000.0)
        val json = objectMapperCustomSerde.writeValueAsString(månedlig)

        assertEquals("400000.0", json)
    }

    @Test
    fun `serialiserer InntektbeløpDto DagligDouble som bare tall`() {
        val daglig = InntektbeløpDto.DagligDouble(1538.46)
        val json = objectMapperCustomSerde.writeValueAsString(daglig)

        assertEquals("1538.46", json)
    }

    @Test
    fun `serialiserer InntektbeløpDto DagligInt som bare tall`() {
        val daglig = InntektbeløpDto.DagligInt(1538)
        val json = objectMapperCustomSerde.writeValueAsString(daglig)

        assertEquals("1538", json)
    }

    @Test
    fun `serialiserer Sykepengegrunnlag med InntektbeløpDto som tall`() {
        val sykepengegrunnlag =
            no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Sykepengegrunnlag(
                grunnbeløp = InntektbeløpDto.Årlig(130160.0),
                totaltInntektsgrunnlag = InntektbeløpDto.Årlig(400000.0),
                sykepengegrunnlag = InntektbeløpDto.Årlig(400000.0),
                seksG = InntektbeløpDto.Årlig(780960.0),
                begrensetTil6G = false,
                grunnbeløpVirkningstidspunkt = java.time.LocalDate.of(2024, 5, 1),
                næringsdel = null,
            )

        val json = objectMapperCustomSerde.writeValueAsString(sykepengegrunnlag)

        // Verifiser at InntektbeløpDto feltene er serialisert som tall, ikke objekter
        assert(json.contains("\"grunnbeløp\":130160.0"))
        assert(json.contains("\"totaltInntektsgrunnlag\":400000.0"))
        assert(json.contains("\"sykepengegrunnlag\":400000.0"))
        assert(json.contains("\"seksG\":780960.0"))

        // Verifiser at det ikke inneholder objektstrukturen {"beløp": ...}
        assert(!json.contains("\"beløp\""))
    }
}
