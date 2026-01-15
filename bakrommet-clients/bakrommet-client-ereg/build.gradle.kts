plugins {
    `java-test-fixtures`
}

dependencies {
    api(platform(project(":bakrommet-dependencies")))
    api(project(":bakrommet-clients:bakrommet-client-common"))

    testFixturesImplementation(project(":bakrommet-clients:bakrommet-client-common"))
    testFixturesImplementation("io.github.serpro69:kotlin-faker:1.16.0")
    testFixturesImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-common")))
}
