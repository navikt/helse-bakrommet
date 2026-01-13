plugins {
    `java-test-fixtures`
}

dependencies {
    api(project(":bakrommet-services"))

    api(platform(project(":bakrommet-dependencies")))

    api("io.ktor:ktor-client-core")
    api("io.ktor:ktor-client-content-negotiation")
    api("io.ktor:ktor-serialization-jackson")
    api("io.ktor:ktor-client-apache-jvm")

    testImplementation("io.ktor:ktor-client-mock-jvm")
    testImplementation("no.nav.security:mock-oauth2-server")

    testFixturesImplementation("io.ktor:ktor-client-mock-jvm")
    testFixturesImplementation("no.nav.security:mock-oauth2-server")
}
