package no.nav.helse.bakrommet.util

import no.nav.helse.bakrommet.errorhandling.InputValideringException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class ObjectMapperTest {
    @Test
    fun `somGyldigUUID mapper til UUID eller thrower`() {
        val enUUID = UUID.randomUUID().toString()
        assertDoesNotThrow { enUUID.somGyldigUUID() }
        assertThrows<InputValideringException> { "${enUUID}a".somGyldigUUID() }
        assertThrows<InputValideringException> { "a$enUUID".somGyldigUUID() }
        assertThrows<InputValideringException> { null.somGyldigUUID() }
    }

    @Test
    fun `dette skulle egentlig ikke ha fungert - UUIDfromString aksepterer ugyldige verdier`() {
        // UUID.fromString sjekker ikke så mye mer enn at strengen inneholder fem bolker med tegn, delt av bindestrek, og makslengde 36
        assertDoesNotThrow { "a-b-c-d-e".somGyldigUUID() }
        assertDoesNotThrow { "222220ef3d18af9-e12d-4a42-9-1f71824f".somGyldigUUID() }
    }
}
