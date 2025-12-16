package no.nav.helse.bakrommet.serde

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.helse.dto.InntektbeløpDto

/**
 * ObjectMapper med custom deserializer og serializer for InntektbeløpDto
 * som håndterer både int og double verdier fra frontend og serialiserer
 * InntektbeløpDto objekter som bare tall i stedet for objekter med beløp-felt.
 */
val objectMapperCustomSerde: ObjectMapper =
    ObjectMapper()
        .registerModule(JavaTimeModule())
        .registerKotlinModule()
        .registerModule(
            SimpleModule().addDeserializer(
                InntektbeløpDto.MånedligDouble::class.java,
                InntektbeløpDtoMånedligDoubleDeserializer(),
            ),
        ).configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
