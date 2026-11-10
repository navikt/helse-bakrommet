plugins {
    id("no.nav.sykepenger.kotlin")
}

dependencies {
    // bruker "api" sånn at avhengigheten blir kopiert ut til konsumenter av denne modulen
    api(project(":sykepenger-primitiver-dto"))

    implementation(libs.jackson.core)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.datatype.jsr310)
}

// Modulen var unntatt ktlint før standardiseringen, og skal ikke reformateres automatisk
ktlint {
    filter {
        exclude { true }
    }
}
