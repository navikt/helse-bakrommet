val jsonassertVersion = "1.5.0"
val tbdSpillAvImMatchingVersion = "2025.04.08-12.41-e519e1f8"

dependencies {
    // bruker "api" sånn at avhengigheten blir kopiert ut til konsumenter av denne modulen
    api(project(":sykepenger-model-dto"))

    api(project(":sykepenger-primitiver"))
    api(project(":sykepenger-utbetaling"))
    testImplementation(kotlin("reflect"))
    testImplementation(testFixtures(project(":sykepenger-utbetaling")))
    testImplementation("org.skyscreamer:jsonassert:$jsonassertVersion")
    testImplementation("com.github.navikt.spill_av_im:matching:$tbdSpillAvImMatchingVersion")

    // for å kunne lage json av spannerpersoner
    testImplementation("com.fasterxml.jackson.core:jackson-core")
}

tasks {
    withType<Test> {
        systemProperty("junit.jupiter.execution.parallel.enabled", "true")
        systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
        systemProperty("junit.jupiter.execution.parallel.config.strategy", "fixed")
        systemProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", "8")
    }
}
