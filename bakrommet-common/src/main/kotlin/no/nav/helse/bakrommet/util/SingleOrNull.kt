package no.nav.helse.bakrommet.util

fun <T> Collection<T>.singleOrNull(predicate: (T) -> Boolean): T? {
    val matches = filter(predicate)
    return when (matches.size) {
        0 -> null
        1 -> matches.first()
        else -> throw IllegalStateException("Expected at most one element matching predicate, but found ${matches.size}")
    }
}
