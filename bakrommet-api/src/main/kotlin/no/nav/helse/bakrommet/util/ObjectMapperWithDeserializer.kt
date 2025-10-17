package no.nav.helse.bakrommet.util

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.helse.dto.InntektbeløpDto

/**
 * ObjectMapper med custom deserializer for InntektbeløpDto.Årlig
 * som håndterer både int og double verdier fra frontend.
 */
val objectMapperWithDeserializer: ObjectMapper =
    ObjectMapper()
        .registerModule(JavaTimeModule())
        .registerKotlinModule()
        .registerModule(
            SimpleModule()
                .addDeserializer(
                    InntektbeløpDto.Årlig::class.java,
                    InntektbeløpDtoÅrligDeserializer(),
                ).addDeserializer(
                    InntektbeløpDto.MånedligDouble::class.java,
                    InntektbeløpDtoMånedligDoubleDeserializer(),
                ).addDeserializer(
                    InntektbeløpDto.DagligInt::class.java,
                    InntektbeløpDtoDagligIntDeserializer(),
                ),
        ).configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
