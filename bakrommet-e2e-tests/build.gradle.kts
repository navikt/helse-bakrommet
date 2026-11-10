plugins {
    id("no.nav.sykepenger.kotlin")
}

dependencies {
    implementation(libs.hikaricp)

    testImplementation(project(":bakrommet-bootstrap"))
    testImplementation(project(":bakrommet-services"))
    testImplementation(project(":bakrommet-api-dto"))
    testImplementation(project(":bakrommet-api"))
    testImplementation(project(":bakrommet-clients:bakrommet-client-aareg"))
    testImplementation(project(":bakrommet-clients:bakrommet-client-ainntekt"))
    testImplementation(project(":bakrommet-clients:bakrommet-client-ereg"))
    testImplementation(project(":bakrommet-clients:bakrommet-client-pdl"))
    testImplementation(project(":bakrommet-clients:bakrommet-client-sigrun"))
    testImplementation(project(":bakrommet-clients:bakrommet-client-inntektsmelding"))
    testImplementation(project(":bakrommet-clients:bakrommet-client-sykepengesoknad"))
    testImplementation(project(":sykepenger-model"))

    testImplementation(libs.kotliquery)

    testImplementation(libs.ktor.server.core)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.core)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.client.mock.jvm)
    testImplementation(libs.ktor.serialization.jackson)
    testImplementation(libs.ktor.client.apache.jvm)
    testImplementation(libs.ktor.server.auth.jwt)

    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.mock.oauth2.server)
    testImplementation(libs.jackson.datatype.jsr310)
    testImplementation(libs.sykepengesoknad.kafka)
    testImplementation(libs.sykepenger.okonomi)

    testImplementation(testFixtures(project(":bakrommet-db")))
    testImplementation(testFixtures(project(":bakrommet-services")))
    testImplementation(testFixtures(project(":bakrommet-domain")))
    testImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-inntektsmelding")))
    testImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-ainntekt")))
    testImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-aareg")))
    testImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-ereg")))
    testImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-sigrun")))
    testImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-sykepengesoknad")))
    testImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-pdl")))
    testImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-obo")))
}
