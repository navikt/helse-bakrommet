plugins {
    `java-test-fixtures`
}

dependencies {
    api(platform(project(":bakrommet-dependencies")))

    api("io.ktor:ktor-client-core")
    api("io.ktor:ktor-client-content-negotiation")
    api("io.ktor:ktor-serialization-jackson")
    api("io.ktor:ktor-client-apache-jvm")
    api("io.ktor:ktor-server-auth-jwt")
    api("io.ktor:ktor-server-status-pages")
    api("io.ktor:ktor-server-core")
    api("com.fasterxml.jackson.core:jackson-core")
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.module:jackson-module-kotlin")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    api("ch.qos.logback:logback-classic")
    api("org.slf4j:slf4j-api")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json")

    testImplementation("io.ktor:ktor-client-mock-jvm")
    testImplementation("no.nav.security:mock-oauth2-server")

    testFixturesImplementation("io.ktor:ktor-client-mock-jvm")
    testFixturesImplementation("no.nav.security:mock-oauth2-server")
}
