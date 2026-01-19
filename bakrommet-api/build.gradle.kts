plugins {
    `java-test-fixtures`
}

dependencies {
    api(platform(project(":bakrommet-dependencies")))

    implementation(project(":bakrommet-api-dto"))
    implementation(project(":bakrommet-services"))

    implementation("no.nav.helse.flex:sykepengesoknad-kafka")
    implementation("com.fasterxml.jackson.core:jackson-databind")

    api("io.ktor:ktor-server-core")
    api("io.ktor:ktor-server-cio")
    implementation("io.ktor:ktor-server-metrics-micrometer")
    implementation("io.ktor:ktor-server-auth")
    implementation("io.ktor:ktor-server-auth-jwt")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-server-call-logging")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("io.ktor:ktor-serialization-jackson")
}
