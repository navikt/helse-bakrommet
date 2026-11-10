plugins {
    id("no.nav.sykepenger.kotlin")
    `java-test-fixtures`
}

dependencies {
    api(project(":bakrommet-domain"))
    api(project(":bakrommet-kafka-dto"))
    api(project(":sykepenger-model"))
    api(libs.kotlinx.coroutines.core)
    // TODO flytte ut til client igjen når vi har en egen domene-representasjon av IM
    api(libs.inntektsmelding.kontrakt)

    // TODO flytte ut til client igjen når vi har en egen domene-representasjon av søknad
    api(libs.sykepengesoknad.kafka)

    implementation(libs.sykepenger.okonomi)
    implementation(libs.logstash.logback.encoder)
    api(libs.logback.classic)
    api(libs.slf4j.api)

    api(libs.jackson.core)
    api(libs.jackson.databind)
    api(libs.jackson.module.kotlin)
    api(libs.jackson.datatype.jsr310)

    testImplementation(testFixtures(project(":bakrommet-services")))

    testFixturesImplementation(kotlin("test"))
    testFixturesImplementation(project(":sykepenger-model"))
    testFixturesImplementation(libs.sykepenger.okonomi)
}
