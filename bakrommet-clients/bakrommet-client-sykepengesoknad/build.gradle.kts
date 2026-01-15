plugins {
    `java-test-fixtures`
}

dependencies {
    api(platform(project(":bakrommet-dependencies")))
    api(project(":bakrommet-clients:bakrommet-client-common"))

    testFixturesImplementation("no.nav.helse.flex:sykepengesoknad-kafka")

    testFixturesImplementation(project(":bakrommet-clients:bakrommet-client-common"))
    testFixturesImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-common")))
}
