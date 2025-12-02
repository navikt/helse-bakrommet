package no.nav.helse.bakrommet.serde

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveText

suspend inline fun <reified T> ApplicationCall.receiveWithCustomMapper(mapper: ObjectMapper): T {
    val json = receiveText()
    return mapper.readValue(json, T::class.java)
}
