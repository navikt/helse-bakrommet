plugins {
    id("no.nav.sykepenger.kotlin")
    `java-test-fixtures`
}

dependencies {
    api(project(":bakrommet-services"))

    api(libs.ktor.client.core)
    api(libs.ktor.client.content.negotiation)
    api(libs.ktor.serialization.jackson)
    api(libs.ktor.client.apache.jvm)
    api(libs.ktor.server.auth.jwt)
    api(libs.ktor.server.status.pages)
    api(libs.ktor.server.core)

    testFixturesApi(libs.ktor.client.mock.jvm)
    testFixturesApi(libs.ktor.client.core)
    testFixturesApi(libs.ktor.client.content.negotiation)
    testFixturesApi(libs.ktor.serialization.jackson)
    testFixturesApi(libs.jackson.datatype.jsr310)
}
