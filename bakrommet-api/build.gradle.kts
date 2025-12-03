plugins {
    `java-test-fixtures`
}

dependencies {
    api(platform(project(":bakrommet-dependencies")))

    implementation(project(":bakrommet-common"))
    implementation(project(":bakrommet-api-dto"))
    implementation(project(":bakrommet-services"))
    implementation("no.nav.helse.flex:sykepengesoknad-kafka")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("io.ktor:ktor-server-core")
}
