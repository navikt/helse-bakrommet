package no.nav.helse.bakrommet.serde

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import no.nav.helse.dto.InntektbeløpDto

class InntektbeløpDtoÅrligSerializer : JsonSerializer<InntektbeløpDto.Årlig>() {
    override fun serialize(
        value: InntektbeløpDto.Årlig,
        gen: JsonGenerator,
        serializers: SerializerProvider,
    ) {
        gen.writeNumber(value.beløp)
    }
}

class InntektbeløpDtoMånedligDoubleSerializer : JsonSerializer<InntektbeløpDto.MånedligDouble>() {
    override fun serialize(
        value: InntektbeløpDto.MånedligDouble,
        gen: JsonGenerator,
        serializers: SerializerProvider,
    ) {
        gen.writeNumber(value.beløp)
    }
}

class InntektbeløpDtoDagligDoubleSerializer : JsonSerializer<InntektbeløpDto.DagligDouble>() {
    override fun serialize(
        value: InntektbeløpDto.DagligDouble,
        gen: JsonGenerator,
        serializers: SerializerProvider,
    ) {
        gen.writeNumber(value.beløp)
    }
}

class InntektbeløpDtoDagligIntSerializer : JsonSerializer<InntektbeløpDto.DagligInt>() {
    override fun serialize(
        value: InntektbeløpDto.DagligInt,
        gen: JsonGenerator,
        serializers: SerializerProvider,
    ) {
        gen.writeNumber(value.beløp)
    }
}
