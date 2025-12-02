package no.nav.helse.bakrommet

import no.nav.helse.bakrommet.api.setupApiRoutes

fun main() {
    startApp(Configuration.fromEnv()) { services ->
        setupApiRoutes(services)
    }
}
