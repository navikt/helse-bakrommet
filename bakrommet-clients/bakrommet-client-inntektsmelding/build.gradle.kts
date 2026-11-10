plugins {
    id("no.nav.sykepenger.kotlin")
    `java-test-fixtures`
}

dependencies {
    api(project(":bakrommet-clients:bakrommet-client-common"))

    testFixturesImplementation(libs.inntektsmelding.kontrakt)

    testFixturesImplementation(project(":bakrommet-clients:bakrommet-client-common"))
    testFixturesImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-common")))
}
