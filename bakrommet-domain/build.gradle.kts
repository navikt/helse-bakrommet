plugins {
    id("no.nav.sykepenger.kotlin")
    `java-test-fixtures`
}

dependencies {
    implementation(libs.sykepenger.okonomi)

    testFixturesApi(kotlin("test"))
}
