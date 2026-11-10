plugins {
    id("no.nav.sykepenger.kotlin")
    `java-test-fixtures`
}

dependencies {
    implementation(project(":bakrommet-api-dto"))
    implementation(project(":bakrommet-services"))

    implementation(libs.sykepengesoknad.kafka)
    implementation(libs.jackson.databind)

    api(libs.ktor.server.core)
    api(libs.ktor.server.cio)
    implementation(libs.ktor.server.metrics.micrometer)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.serialization.jackson)
}
