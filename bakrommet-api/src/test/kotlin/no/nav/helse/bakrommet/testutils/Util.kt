package no.nav.helse.bakrommet.testutils

fun <T> T.print(prefix: String = "") = this.also { println(prefix + this) }
