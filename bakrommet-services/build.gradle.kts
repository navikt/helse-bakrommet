plugins {
    `java-test-fixtures`
}

val shedlockVersion = "7.2.2"

dependencies {
    api(platform(project(":bakrommet-dependencies")))

    api(project(":bakrommet-common"))
    api(project(":bakrommet-kafka-dto"))
    api(project(":bakrommet-clients:bakrommet-client-pdl"))
    api(project(":bakrommet-clients:bakrommet-client-aareg"))
    api(project(":bakrommet-clients:bakrommet-client-ainntekt"))
    api(project(":bakrommet-clients:bakrommet-client-ereg"))
    api(project(":bakrommet-clients:bakrommet-client-sigrun"))
    api(project(":bakrommet-clients:bakrommet-client-inntektsmelding"))
    api(project(":bakrommet-clients:bakrommet-client-sykepengesoknad"))
    api(project(":sykepenger-model"))

    api("io.ktor:ktor-server-metrics-micrometer")
    api("io.ktor:ktor-server-core")
    api("io.ktor:ktor-server-cio")
    api("io.ktor:ktor-server-auth")
    api("io.ktor:ktor-server-auth-jwt")
    api("io.ktor:ktor-server-content-negotiation")
    api("io.ktor:ktor-server-call-logging")
    api("io.ktor:ktor-server-status-pages")
    api("io.ktor:ktor-serialization-jackson")

    implementation("net.javacrumbs.shedlock:shedlock-core:$shedlockVersion")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc:$shedlockVersion")

    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    api("org.postgresql:postgresql")
    api("com.zaxxer:HikariCP")
    api("org.flywaydb:flyway-core")
    api("org.flywaydb:flyway-database-postgresql")
    api("com.github.seratch:kotliquery")

    implementation("org.apache.kafka:kafka-clients")
    implementation("no.nav.helse.flex:sykepengesoknad-kafka")
    implementation("com.github.navikt.spleis:sykepenger-okonomi")

    implementation("ch.qos.logback:logback-classic")
    implementation("net.logstash.logback:logstash-logback-encoder")
    implementation("org.slf4j:slf4j-api")

    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("no.nav.security:mock-oauth2-server")
    testImplementation(testFixtures(project(":bakrommet-services")))
    testImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-pdl")))
    testImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-sykepengesoknad")))
    testImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-aareg")))
    testImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-ainntekt")))
    testImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-sigrun")))

    testFixturesImplementation("io.ktor:ktor-server-test-host")
    testFixturesImplementation("org.testcontainers:postgresql")
    testFixturesImplementation("no.nav.security:mock-oauth2-server")
    testFixturesImplementation("com.zaxxer:HikariCP")
    testFixturesImplementation("org.postgresql:postgresql")
    testFixturesImplementation("org.flywaydb:flyway-core")
    testFixturesImplementation("org.flywaydb:flyway-database-postgresql")
    testFixturesImplementation("com.github.seratch:kotliquery")
    testFixturesImplementation(project(":bakrommet-common"))
    testFixturesImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-pdl")))
    testFixturesImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-aareg")))
    testFixturesImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-ainntekt")))
    testFixturesImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-inntektsmelding")))
    testFixturesImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-sigrun")))
    testFixturesImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-sykepengesoknad")))
    testFixturesImplementation(project(":sykepenger-model"))
    testFixturesImplementation("com.github.navikt.spleis:sykepenger-okonomi")
}
