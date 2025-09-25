plugins {
    `java-test-fixtures`
}

dependencies {
    api(platform(project(":bakrommet-dependencies")))

    api(project(":bakrommet-common"))
    implementation(project(":bakrommet-client-pdl"))
    implementation(project(":bakrommet-client-aareg"))
    implementation(project(":bakrommet-client-ainntekt"))
    implementation(project(":bakrommet-client-sigrun"))
    implementation(project(":bakrommet-client-inntektsmelding"))
    implementation(project(":bakrommet-client-sykepengesoknad"))
    implementation(project(":sykepenger-model"))

    implementation("io.ktor:ktor-server-metrics-micrometer")
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-cio")
    implementation("io.ktor:ktor-server-auth")
    implementation("io.ktor:ktor-server-auth-jwt")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-server-call-logging")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("io.ktor:ktor-serialization-jackson")

    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    implementation("org.postgresql:postgresql")
    implementation("com.zaxxer:HikariCP")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("com.github.seratch:kotliquery")

    implementation("org.apache.kafka:kafka-clients")
    implementation("no.nav.helse.flex:sykepengesoknad-kafka")
    implementation("com.github.navikt.spleis:sykepenger-okonomi")

    implementation("ch.qos.logback:logback-classic")
    implementation("net.logstash.logback:logstash-logback-encoder")
    implementation("org.slf4j:slf4j-api")

    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("no.nav.security:mock-oauth2-server")

    testFixturesImplementation("io.ktor:ktor-server-test-host")
    testFixturesImplementation("org.testcontainers:postgresql")
    testFixturesImplementation("no.nav.security:mock-oauth2-server")
    testFixturesImplementation(testFixtures(project(":bakrommet-client-pdl")))
    testFixturesImplementation(testFixtures(project(":bakrommet-client-aareg")))
    testFixturesImplementation(testFixtures(project(":bakrommet-client-ainntekt")))
    testFixturesImplementation(testFixtures(project(":bakrommet-client-inntektsmelding")))
    testFixturesImplementation(testFixtures(project(":bakrommet-client-sigrun")))
    testFixturesImplementation(testFixtures(project(":bakrommet-client-sykepengesoknad")))
}
