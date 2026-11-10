plugins {
    id("no.nav.sykepenger.kotlin")
    `java-test-fixtures`
}

dependencies {
    api(project(":bakrommet-clients:bakrommet-client-common"))

    testFixturesImplementation(project(":bakrommet-clients:bakrommet-client-common"))
    testFixturesImplementation(libs.kotlin.faker)
    testFixturesImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-common")))
}
