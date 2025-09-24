plugins {
    `java-test-fixtures`
}

val ktorVersion = "3.3.0"

dependencies {
    api("io.ktor:ktor-client-core:$ktorVersion")
    api("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    api("io.ktor:ktor-serialization-jackson:$ktorVersion")
    api("io.ktor:ktor-client-apache-jvm:$ktorVersion")
    api("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    api("io.ktor:ktor-server-status-pages:$ktorVersion")
    api("io.ktor:ktor-server-core:$ktorVersion")
    api("com.fasterxml.jackson.core:jackson-core:2.18.2")
    api("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")
    api("ch.qos.logback:logback-classic:1.5.18")
    api("org.slf4j:slf4j-api:2.0.17")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    testImplementation("io.ktor:ktor-client-mock-jvm:$ktorVersion")
    testImplementation("no.nav.security:mock-oauth2-server:2.3.0")

    testFixturesImplementation("io.ktor:ktor-client-mock-jvm:$ktorVersion")
    testFixturesImplementation("no.nav.security:mock-oauth2-server:2.3.0")
}
