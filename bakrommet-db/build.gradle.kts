plugins {
    id("no.nav.sykepenger.kotlin")
    `java-test-fixtures`
}

dependencies {
    api(project(":bakrommet-services"))
    implementation(project(":bakrommet-db-dto"))

    api(libs.postgresql)
    api(libs.hikaricp)
    api(libs.flyway.core)
    api(libs.flyway.database.postgresql)
    api(libs.kotliquery)

    implementation(libs.shedlock.core)
    implementation(libs.shedlock.provider.jdbc)

    implementation(libs.logback.classic)
    implementation(libs.logstash.logback.encoder)
    implementation(libs.slf4j.api)

    implementation(libs.micrometer.registry.prometheus)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.mock.oauth2.server)
    testImplementation(testFixtures(project(":bakrommet-services")))
    testImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-pdl")))
    testImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-sykepengesoknad")))
    testImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-aareg")))
    testImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-ainntekt")))
    testImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-sigrun")))
    testImplementation(testFixtures(project(":bakrommet-domain")))

    testFixturesApi(libs.testcontainers.postgresql)
    testFixturesApi(libs.hikaricp)
    testFixturesApi(libs.postgresql)
    testFixturesApi(libs.flyway.core)
    testFixturesApi(libs.flyway.database.postgresql)
    testFixturesApi(libs.kotliquery)
}
