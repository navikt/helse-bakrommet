package no.nav.helse.bakrommet.util

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonMappingException
import no.nav.helse.dto.InntektbeløpDto

/**
 * Custom deserializer for InntektbeløpDto.Årlig som håndterer både int og double verdier.
 * Dette løser problemet når frontend sender hele tall uten desimaler.
 */
class InntektbeløpDtoÅrligDeserializer : JsonDeserializer<InntektbeløpDto.Årlig>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ): InntektbeløpDto.Årlig =
        when (p.currentToken) {
            JsonToken.VALUE_NUMBER_INT -> InntektbeløpDto.Årlig(p.intValue.toDouble())
            JsonToken.VALUE_NUMBER_FLOAT -> InntektbeløpDto.Årlig(p.doubleValue)
            JsonToken.START_OBJECT -> {
                // Håndter objekt med beløp-felt: {"beløp": 100000}
                var beløp: Double? = null
                p.nextToken() // Gå til første felt
                while (p.currentToken != JsonToken.END_OBJECT) {
                    if (p.currentName == "beløp") {
                        p.nextToken()
                        beløp =
                            when (p.currentToken) {
                                JsonToken.VALUE_NUMBER_INT -> p.intValue.toDouble()
                                JsonToken.VALUE_NUMBER_FLOAT -> p.doubleValue
                                else -> throw JsonMappingException(p, "Expected number for beløp field")
                            }
                    } else {
                        p.nextToken()
                        p.skipChildren() // Hopp over ukjente felter
                    }
                    p.nextToken()
                }
                if (beløp == null) {
                    throw JsonMappingException(p, "Missing beløp field in InntektbeløpDto.Årlig")
                }
                InntektbeløpDto.Årlig(beløp)
            }
            else -> throw JsonMappingException(p, "Expected number or object with beløp field")
        }
}

/**
 * Custom deserializer for InntektbeløpDto.MånedligDouble som håndterer både int og double verdier.
 * Dette løser problemet når frontend sender hele tall uten desimaler.
 */
class InntektbeløpDtoMånedligDoubleDeserializer : JsonDeserializer<InntektbeløpDto.MånedligDouble>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ): InntektbeløpDto.MånedligDouble =
        when (p.currentToken) {
            JsonToken.VALUE_NUMBER_INT -> InntektbeløpDto.MånedligDouble(p.intValue.toDouble())
            JsonToken.VALUE_NUMBER_FLOAT -> InntektbeløpDto.MånedligDouble(p.doubleValue)
            JsonToken.START_OBJECT -> {
                // Håndter objekt med beløp-felt: {"beløp": 100000}
                var beløp: Double? = null
                p.nextToken() // Gå til første felt
                while (p.currentToken != JsonToken.END_OBJECT) {
                    if (p.currentName == "beløp") {
                        p.nextToken()
                        beløp =
                            when (p.currentToken) {
                                JsonToken.VALUE_NUMBER_INT -> p.intValue.toDouble()
                                JsonToken.VALUE_NUMBER_FLOAT -> p.doubleValue
                                else -> throw JsonMappingException(p, "Expected number for beløp field")
                            }
                    } else {
                        p.nextToken()
                        p.skipChildren() // Hopp over ukjente felter
                    }
                    p.nextToken()
                }
                if (beløp == null) {
                    throw JsonMappingException(p, "Missing beløp field in InntektbeløpDto.MånedligDouble")
                }
                InntektbeløpDto.MånedligDouble(beløp)
            }
            else -> throw JsonMappingException(p, "Expected number or object with beløp field")
        }
}
