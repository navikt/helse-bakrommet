plugins {
    `java-test-fixtures`
}

val shedlockVersion = "7.2.2"

dependencies {
    api(platform(project(":bakrommet-dependencies")))

    api(project(":bakrommet-domain"))
    api(project(":bakrommet-kafka-dto"))
    api(project(":sykepenger-model"))
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    // TODO flytte ut til client igjen når vi har en egen domene-representasjon av IM
    api("no.nav.sykepenger.kontrakter:inntektsmelding-kontrakt:2025.04.04-01-56-365d3")

    // TODO flytte ut til client igjen når vi har en egen domene-representasjon av søknad
    api("no.nav.helse.flex:sykepengesoknad-kafka")

    implementation("com.github.navikt.spleis:sykepenger-okonomi")

    implementation("com.github.navikt.spleis:sykepenger-okonomi")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("net.logstash.logback:logstash-logback-encoder")
    api("ch.qos.logback:logback-classic")
    api("org.slf4j:slf4j-api")

    api("com.fasterxml.jackson.core:jackson-core")
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.module:jackson-module-kotlin")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    testImplementation(testFixtures(project(":bakrommet-services")))

    testFixturesImplementation(kotlin("test"))
    testFixturesImplementation(project(":sykepenger-model"))
    testFixturesImplementation("com.github.navikt.spleis:sykepenger-okonomi")
}
