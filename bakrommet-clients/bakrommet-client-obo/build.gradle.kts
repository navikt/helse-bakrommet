plugins {
    `java-test-fixtures`
}

dependencies {
    api(platform(project(":bakrommet-dependencies")))
    api(project(":bakrommet-clients:bakrommet-client-common"))

    testImplementation("io.ktor:ktor-client-mock-jvm")
    testImplementation("no.nav.security:mock-oauth2-server")

    testFixturesImplementation("io.ktor:ktor-client-mock-jvm")
    testFixturesImplementation("no.nav.security:mock-oauth2-server")

    testFixturesImplementation(project(":bakrommet-clients:bakrommet-client-common"))
    testFixturesImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-common")))
}
