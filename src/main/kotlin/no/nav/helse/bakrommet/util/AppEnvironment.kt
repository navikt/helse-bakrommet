package no.nav.helse.bakrommet.util

import java.time.OffsetDateTime

object AppEnvironment {
    fun podname(): String = systemEnvOrDefault("HOSTNAME", "unknownHost")

    fun appname(): String = systemEnvOrDefault("NAIS_APP_NAME", "unknownApp")

    fun imagename(): String = systemEnvOrDefault("NAIS_APP_IMAGE", "unknownImage")

    fun appInstanceInfo(): Map<String, String> =
        mapOf(
            "podname" to podname(),
            "appname" to appname(),
            "imagename" to imagename(),
        )

    fun callsiteInfo(t: Throwable) = appInstanceInfo() + Pair("callsite", t.stackTrace[0].toString())
}

data class Kildespor(val kilde: String) {
    companion object {
        fun fraHer(
            t: Throwable,
            vararg params: Any,
        ) = Kildespor(
            (
                AppEnvironment.callsiteInfo(t) +
                    Pair("params", params.map { it.toString() }) +
                    Pair("instant", OffsetDateTime.now().toString())
            ).toString(),
        )
    }
}

private fun systemEnvOrDefault(
    name: String,
    default: String,
) = System.getenv(name) ?: default
