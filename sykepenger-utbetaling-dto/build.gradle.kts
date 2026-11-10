plugins {
    id("no.nav.sykepenger.kotlin")
}

dependencies {
    // bruker "implementation" fremfor "api" for å unngå
    // at avhengigheten blir transitiv, altså kopiert ut, til de som bruker denne modulen.
    implementation(project(":sykepenger-primitiver-dto"))
    implementation(libs.jackson.core)
    implementation(libs.jackson.annotations)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.sykepenger.okonomi)
}

// Modulen var unntatt ktlint før standardiseringen, og skal ikke reformateres automatisk
ktlint {
    filter {
        exclude { true }
    }
}
