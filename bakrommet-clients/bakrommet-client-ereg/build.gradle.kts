plugins {
    `java-test-fixtures`
}

dependencies {
    api(platform(project(":bakrommet-dependencies")))
    api(project(":bakrommet-common"))
    api(project(":bakrommet-services"))

    testFixturesImplementation(testFixtures(project(":bakrommet-common")))
    testFixturesImplementation("io.ktor:ktor-client-mock-jvm")
    testFixturesImplementation("io.ktor:ktor-client-core")
    testFixturesImplementation("io.ktor:ktor-client-content-negotiation")
    testFixturesImplementation("io.ktor:ktor-serialization-jackson")
    testFixturesImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    testFixturesImplementation("io.github.serpro69:kotlin-faker:1.16.0")
}
