plugins {
    id("no.nav.sykepenger.kotlin")
    `java-test-fixtures`
}

dependencies {
    api(project(":bakrommet-clients:bakrommet-client-common"))

    testImplementation(libs.ktor.client.mock.jvm)
    testImplementation(libs.mock.oauth2.server)

    testFixturesImplementation(libs.ktor.client.mock.jvm)
    testFixturesImplementation(libs.mock.oauth2.server)

    testFixturesImplementation(project(":bakrommet-clients:bakrommet-client-common"))
    testFixturesImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-common")))
}
