plugins {
    `java-test-fixtures`
}

val ktorVersion = "3.3.0"
val flywayVersion = "11.13.1"

dependencies {
    api(project(":bakrommet-common"))
    implementation(project(":bakrommet-client-pdl"))
    implementation(project(":bakrommet-client-aareg"))
    implementation(project(":bakrommet-client-ainntekt"))
    implementation(project(":bakrommet-client-sigrun"))
    implementation(project(":bakrommet-client-inntektsmelding"))
    implementation(project(":bakrommet-client-sykepengesoknad"))

    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-cio:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")

    implementation("io.micrometer:micrometer-registry-prometheus:1.15.4")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")

    implementation("org.postgresql:postgresql:42.7.5")
    implementation("com.zaxxer:HikariCP:6.3.0")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")
    implementation("com.github.seratch:kotliquery:1.9.1")

    implementation("org.apache.kafka:kafka-clients:3.8.0")
    implementation("no.nav.helse.flex:sykepengesoknad-kafka:2025.09.09-07.30-baf456bb")
    implementation("com.github.navikt.spleis:sykepenger-okonomi:2025.09.05-14.51-15db36a7")

    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("net.logstash.logback:logstash-logback-encoder:8.1")
    implementation("org.slf4j:slf4j-api:2.0.17")

    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.testcontainers:postgresql:1.21.0")
    testImplementation("no.nav.security:mock-oauth2-server:2.3.0")

    testFixturesImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testFixturesImplementation("org.testcontainers:postgresql:1.21.0")
    testFixturesImplementation("no.nav.security:mock-oauth2-server:2.3.0")
    testFixturesImplementation(testFixtures(project(":bakrommet-client-pdl")))
    testFixturesImplementation(testFixtures(project(":bakrommet-client-aareg")))
    testFixturesImplementation(testFixtures(project(":bakrommet-client-ainntekt")))
    testFixturesImplementation(testFixtures(project(":bakrommet-client-inntektsmelding")))
    testFixturesImplementation(testFixtures(project(":bakrommet-client-sigrun")))
    testFixturesImplementation(testFixtures(project(":bakrommet-client-sykepengesoknad")))
}
