plugins {
    id("no.nav.sykepenger.kotlin")
    `java-test-fixtures`
}

dependencies {
    implementation(project(":bakrommet-services"))

    api(libs.kafka.clients)
}
