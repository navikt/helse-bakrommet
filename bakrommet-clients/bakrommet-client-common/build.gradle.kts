plugins {
    `java-test-fixtures`
}

dependencies {
    api(platform(project(":bakrommet-dependencies")))
    api(project(":bakrommet-services"))

    api("io.ktor:ktor-client-core")
    api("io.ktor:ktor-client-content-negotiation")
    api("io.ktor:ktor-serialization-jackson")
    api("io.ktor:ktor-client-apache-jvm")
    api("io.ktor:ktor-server-auth-jwt")
    api("io.ktor:ktor-server-status-pages")
    api("io.ktor:ktor-server-core")

    testFixturesApi("io.ktor:ktor-client-mock-jvm")
    testFixturesApi("io.ktor:ktor-client-core")
    testFixturesApi("io.ktor:ktor-client-content-negotiation")
    testFixturesApi("io.ktor:ktor-serialization-jackson")
    testFixturesApi("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}
