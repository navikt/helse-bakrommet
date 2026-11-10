plugins {
    id("no.nav.sykepenger.kotlin")
    `java-test-fixtures`
}

dependencies {
    api(libs.commons.codec)

    // bruker "api" sånn at avhengigheten blir kopiert ut til konsumenter av denne modulen
    api(project(":sykepenger-utbetaling-dto"))

    implementation(project(":sykepenger-primitiver"))
    implementation(libs.sykepenger.okonomi)
    testFixturesImplementation(libs.sykepenger.okonomi)
    testFixturesImplementation(project(":sykepenger-primitiver"))
    implementation(libs.slf4j.api)
}

// Modulen var unntatt ktlint før standardiseringen, og skal ikke reformateres automatisk
ktlint {
    filter {
        exclude { true }
    }
}
