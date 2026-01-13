plugins {
    `java-test-fixtures`
}

dependencies {
    api(platform(project(":bakrommet-dependencies")))
    api(project(":bakrommet-common"))
    api(project(":bakrommet-services"))

    testFixturesImplementation("io.ktor:ktor-client-mock-jvm")
    testFixturesImplementation("io.ktor:ktor-client-core")
    testFixturesImplementation("io.ktor:ktor-client-content-negotiation")
    testFixturesImplementation("io.ktor:ktor-serialization-jackson")
    testFixturesImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}
