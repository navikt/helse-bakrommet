package no.nav.helse.spotlight

import org.slf4j.MDC

fun withMDC(
    map: Map<String, Any>,
    block: () -> Unit,
) {
    try {
        map.entries.forEach { (key, value) -> MDC.put(key, value.toString()) }
        block()
    } finally {
        map.keys.forEach(MDC::remove)
    }
}
