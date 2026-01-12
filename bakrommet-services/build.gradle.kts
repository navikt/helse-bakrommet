plugins {
    `java-test-fixtures`
}

val shedlockVersion = "7.2.2"

dependencies {
    api(platform(project(":bakrommet-dependencies")))

    api(project(":bakrommet-common"))
    api(project(":bakrommet-kafka-dto"))
    api(project(":bakrommet-clients:bakrommet-client-pdl"))
    api(project(":bakrommet-clients:bakrommet-client-ereg"))
    api(project(":bakrommet-clients:bakrommet-client-sigrun"))
    api(project(":bakrommet-clients:bakrommet-client-sykepengesoknad"))
    api(project(":sykepenger-model"))

    // TODO flytte ut til client igjen når vi har en egen domene-representasjon av IM
    api("no.nav.sykepenger.kontrakter:inntektsmelding-kontrakt:2025.04.04-01-56-365d3")

    implementation("no.nav.helse.flex:sykepengesoknad-kafka")
    implementation("com.github.navikt.spleis:sykepenger-okonomi")

    implementation("ch.qos.logback:logback-classic")
    implementation("net.logstash.logback:logstash-logback-encoder")
    implementation("org.slf4j:slf4j-api")

    testImplementation(testFixtures(project(":bakrommet-services")))
    testImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-pdl")))
    testImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-sykepengesoknad")))
    testImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-aareg")))
    testImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-sigrun")))

    testFixturesImplementation(kotlin("test"))
    testFixturesImplementation(project(":bakrommet-common"))
    testFixturesImplementation(project(":sykepenger-model"))
    testFixturesImplementation("com.github.navikt.spleis:sykepenger-okonomi")
}
