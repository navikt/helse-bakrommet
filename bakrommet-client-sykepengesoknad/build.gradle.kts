plugins {
    `java-test-fixtures`
}

dependencies {
    api(platform(project(":bakrommet-dependencies")))
    api(project(":bakrommet-common"))
    implementation("no.nav.helse.flex:sykepengesoknad-kafka")

    testFixturesImplementation(testFixtures(project(":bakrommet-common")))
}
