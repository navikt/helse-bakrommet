plugins {
    id("no.nav.sykepenger.kotlin")
    `java-test-fixtures`
}

dependencies {
    api(project(":bakrommet-clients:bakrommet-client-common"))

    testFixturesImplementation(libs.sykepengesoknad.kafka)

    testFixturesImplementation(project(":bakrommet-clients:bakrommet-client-common"))
    testFixturesImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-common")))
}
