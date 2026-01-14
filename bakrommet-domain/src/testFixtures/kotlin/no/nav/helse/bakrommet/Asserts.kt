package no.nav.helse.bakrommet

import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals

fun assertInstantEquals(
    expected: Instant,
    actual: Instant,
) {
    val expectedTruncated = expected.truncatedTo(ChronoUnit.MILLIS)
    val actualTruncated = actual.truncatedTo(ChronoUnit.MILLIS)
    assertEquals(expectedTruncated, actualTruncated)
}
