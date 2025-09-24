plugins {
    `java-test-fixtures`
}

dependencies {
    api(project(":bakrommet-common"))
    implementation("no.nav.helse.flex:sykepengesoknad-kafka:2025.09.09-07.30-baf456bb")

    testFixturesImplementation(testFixtures(project(":bakrommet-common")))
}
