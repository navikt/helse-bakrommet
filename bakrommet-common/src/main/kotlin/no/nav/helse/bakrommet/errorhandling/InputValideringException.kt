package no.nav.helse.bakrommet.errorhandling

import java.lang.RuntimeException

open class InputValideringException(
    message: String,
) : RuntimeException(message)

inline fun <T> medInputvalidering(block: () -> T): T {
    try {
        return block()
    } catch (opprinneligFeil: Exception) {
        var e: Throwable? = opprinneligFeil
        var count = 0
        while (e != null) {
            if (e is InputValideringException) {
                throw e
            }
            if (count > 20) break
            e = e.cause
            count++
        }
        throw opprinneligFeil
    }
}
