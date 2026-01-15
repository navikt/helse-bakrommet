package no.nav.helse.bakrommet.util

import no.nav.helse.bakrommet.Kildespor
import no.nav.helse.bakrommet.client.common.ApplicationConfig
import java.time.OffsetDateTime

fun ApplicationConfig.fraHer(
    t: Throwable,
    vararg params: Any,
) = Kildespor(
    (
        callsiteInfo(t) +
            Pair("params", params.map { it.toString() }) +
            Pair("instant", OffsetDateTime.now().toString())
    ).toString(),
)

fun Kildespor.medTillegg(data: Map<String, String>): Kildespor = Kildespor(kilde + "," + data.toString())
