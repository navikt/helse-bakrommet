plugins {
    `java-test-fixtures`
}

val shedlockVersion = "7.2.2"

dependencies {
    api(platform(project(":bakrommet-dependencies")))

    api(project(":bakrommet-common"))
    api(project(":bakrommet-kafka-dto"))
    api(project(":bakrommet-clients:bakrommet-client-pdl"))
    api(project(":bakrommet-clients:bakrommet-client-aareg"))
    api(project(":bakrommet-clients:bakrommet-client-ainntekt"))
    api(project(":bakrommet-clients:bakrommet-client-ereg"))
    api(project(":bakrommet-clients:bakrommet-client-sigrun"))
    api(project(":bakrommet-clients:bakrommet-client-inntektsmelding"))
    api(project(":bakrommet-clients:bakrommet-client-sykepengesoknad"))
    api(project(":sykepenger-model"))

    implementation("net.javacrumbs.shedlock:shedlock-core:$shedlockVersion")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc:$shedlockVersion")

    implementation("org.apache.kafka:kafka-clients")
    implementation("no.nav.helse.flex:sykepengesoknad-kafka")
    implementation("com.github.navikt.spleis:sykepenger-okonomi")

    implementation("ch.qos.logback:logback-classic")
    implementation("net.logstash.logback:logstash-logback-encoder")
    implementation("org.slf4j:slf4j-api")

    testImplementation(testFixtures(project(":bakrommet-services")))
    testImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-pdl")))
    testImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-sykepengesoknad")))
    testImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-aareg")))
    testImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-ainntekt")))
    testImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-sigrun")))

    testFixturesImplementation(project(":bakrommet-common"))
    testFixturesImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-pdl")))
    testFixturesImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-aareg")))
    testFixturesImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-ainntekt")))
    testFixturesImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-inntektsmelding")))
    testFixturesImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-sigrun")))
    testFixturesImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-sykepengesoknad")))
    testFixturesImplementation(project(":sykepenger-model"))
    testFixturesImplementation("com.github.navikt.spleis:sykepenger-okonomi")
}
