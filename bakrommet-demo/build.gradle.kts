plugins {
    id("no.nav.sykepenger.deployable")
}

sykepengerDeployable {
    mainClass = "no.nav.helse.bakrommet.StartDemoAppKt"
    imageName = "sp-bakrommet"
}

dependencies {
    implementation(project(":bakrommet-bootstrap"))

    implementation(libs.sykepengesoknad.kafka)
    implementation(libs.bundles.ktor.server)

    implementation(testFixtures(project(":bakrommet-services")))
    implementation(testFixtures(project(":bakrommet-clients:bakrommet-client-inntektsmelding")))
    implementation(testFixtures(project(":bakrommet-clients:bakrommet-client-ainntekt")))
    implementation(testFixtures(project(":bakrommet-clients:bakrommet-client-aareg")))
    implementation(testFixtures(project(":bakrommet-clients:bakrommet-client-ereg")))
    implementation(testFixtures(project(":bakrommet-clients:bakrommet-client-sykepengesoknad")))
    implementation(testFixtures(project(":bakrommet-clients:bakrommet-client-sigrun")))
    implementation(testFixtures(project(":bakrommet-clients:bakrommet-client-pdl")))

    implementation(libs.kotlin.faker)
}
