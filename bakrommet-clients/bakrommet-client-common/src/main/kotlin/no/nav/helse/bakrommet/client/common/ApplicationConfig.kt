package no.nav.helse.bakrommet.client.common

class ApplicationConfig(
    podName: String,
    appName: String,
    imageName: String,
) {
    private val appInstanceInfo =
        mapOf(
            "podname" to podName,
            "appname" to appName,
            "imagename" to imageName,
        )

    fun callsiteInfo(t: Throwable) =
        appInstanceInfo +
            Pair(
                "callsite",
                t.stackTrace
                    .filter {
                        it.toString().let { str ->
                            str.contains(".kt:") && str.contains("no.nav.helse.bakrommet")
                        }
                    }.take(3)
                    .toString(),
            )
}
