package no.nav.helse.bakrommet

import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals

fun assertInstantEquals(
    expected: Instant,
    actual: Instant,
) {
    val expectedTruncated = expected.truncatedTo(ChronoUnit.MICROS)
    val actualTruncated = actual.truncatedTo(ChronoUnit.MICROS)
    assertEquals(expectedTruncated, actualTruncated)
}
