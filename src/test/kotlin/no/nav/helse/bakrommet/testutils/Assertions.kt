package no.nav.helse.bakrommet.testutils

import org.junit.jupiter.api.Assertions.*

infix fun <T> T.`should equal`(expected: T) = assertEquals(expected, this, "Expected <$this> to be equal to <$expected>")

infix fun <T> T.`should not equal`(other: T) = assertNotEquals(other, this, "Expected <$this> not to be equal to <$other>")

infix fun <T> Iterable<T>.`should contain`(element: T) = assertTrue(this.contains(element), "Expected <$this> to contain <$element>")

infix fun <T> Iterable<T>.`should not contain`(element: T) =
    assertFalse(this.contains(element), "Expected <$this> not to contain <$element>")
