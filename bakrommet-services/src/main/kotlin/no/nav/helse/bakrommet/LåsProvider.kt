package no.nav.helse.bakrommet

import java.time.Duration

interface LåsProvider {
    fun <T : Any> kjørMedLås(
        iMinst: Duration,
        maksimalt: Duration,
        block: () -> T,
    ): T
}
