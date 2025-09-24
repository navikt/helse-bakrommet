dependencies {
    api(platform(project(":bakrommet-dependencies")))

    testImplementation(project(":bakrommet-bootstrap"))
    testImplementation(project(":bakrommet-api"))
    testImplementation(project(":bakrommet-common"))
    testImplementation(project(":bakrommet-client-aareg"))
    testImplementation(project(":bakrommet-client-ainntekt"))
    testImplementation(project(":bakrommet-client-pdl"))
    testImplementation(project(":bakrommet-client-sigrun"))
    testImplementation(project(":bakrommet-client-inntektsmelding"))
    testImplementation(project(":bakrommet-client-sykepengesoknad"))

    testImplementation("io.ktor:ktor-server-core")
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("io.ktor:ktor-client-core")
    testImplementation("io.ktor:ktor-client-content-negotiation")
    testImplementation("io.ktor:ktor-client-mock-jvm")
    testImplementation("io.ktor:ktor-serialization-jackson")
    testImplementation("io.ktor:ktor-client-apache-jvm")
    testImplementation("io.ktor:ktor-server-auth-jwt")

    testImplementation("org.testcontainers:postgresql")
    testImplementation("no.nav.security:mock-oauth2-server")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    testImplementation("no.nav.helse.flex:sykepengesoknad-kafka")
    testImplementation("com.github.navikt.spleis:sykepenger-okonomi")

    testImplementation(testFixtures(project(":bakrommet-api")))
    testImplementation(testFixtures(project(":bakrommet-common")))
}
