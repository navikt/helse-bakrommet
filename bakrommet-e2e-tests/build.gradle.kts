val ktorVersion = "3.3.0"
val testcontainersVersion = "1.21.0"

dependencies {
    testImplementation(project(":bakrommet-bootstrap"))
    testImplementation(project(":bakrommet-api"))
    testImplementation(project(":bakrommet-common"))
    testImplementation(project(":bakrommet-client-aareg"))
    testImplementation(project(":bakrommet-client-ainntekt"))
    testImplementation(project(":bakrommet-client-pdl"))
    testImplementation(project(":bakrommet-client-sigrun"))
    testImplementation(project(":bakrommet-client-inntektsmelding"))
    testImplementation(project(":bakrommet-client-sykepengesoknad"))

    testImplementation("io.ktor:ktor-server-core:$ktorVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("io.ktor:ktor-client-core:$ktorVersion")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    testImplementation("io.ktor:ktor-client-mock-jvm:$ktorVersion")
    testImplementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    testImplementation("io.ktor:ktor-client-apache-jvm:$ktorVersion")
    testImplementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")

    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("no.nav.security:mock-oauth2-server:2.3.0")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")
    testImplementation("no.nav.helse.flex:sykepengesoknad-kafka:2025.09.09-07.30-baf456bb")
    testImplementation("com.github.navikt.spleis:sykepenger-okonomi:2025.09.05-14.51-15db36a7")

    testImplementation(testFixtures(project(":bakrommet-api")))
    testImplementation(testFixtures(project(":bakrommet-common")))
}
