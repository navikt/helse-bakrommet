package no.nav.helse.bakrommet.util

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import java.util.*

val objectMapper: ObjectMapper =
    ObjectMapper()
        .registerModule(JavaTimeModule())
        .registerKotlinModule()
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

fun Any.serialisertTilString(): String = objectMapper.writeValueAsString(this)

fun <T> T.tilJsonNode(): JsonNode = objectMapper.valueToTree(this)

fun String.asJsonNode(): JsonNode {
    return objectMapper.readTree(this)
}

fun Any.toJsonNode(): JsonNode {
    return objectMapper.valueToTree(this)
}

inline fun <reified T> String.somListe(): List<T> {
    return objectMapper.readValue(
        this,
        objectMapper.typeFactory.constructCollectionType(
            List::class.java,
            T::class.java,
        ),
    )
}

inline fun <reified T> JsonNode.deserialize(): T = objectMapper.convertValue(this, T::class.java)

fun String?.somGyldigUUID(): UUID {
    if (this == null) throw InputValideringException("Ugyldig UUID. Forventet UUID-format")
    return try {
        UUID.fromString(this)
    } catch (ex: IllegalArgumentException) {
        throw InputValideringException("Ugyldig UUID. Forventet UUID-format")
    }
}
