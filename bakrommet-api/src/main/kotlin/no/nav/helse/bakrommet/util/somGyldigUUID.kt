package no.nav.helse.bakrommet.util

import no.nav.helse.bakrommet.errorhandling.InputValideringException
import java.util.UUID

fun String?.somGyldigUUID(): UUID {
    if (this == null) throw InputValideringException("Ugyldig UUID. Forventet UUID-format. Fant $this")
    return try {
        UUID.fromString(this)
    } catch (_: IllegalArgumentException) {
        throw InputValideringException("Ugyldig UUID. Forventet UUID-format.  Fant $this")
    }
}
