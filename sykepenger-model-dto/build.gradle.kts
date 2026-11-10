plugins {
    id("no.nav.sykepenger.kotlin")
}

dependencies {
    // bruker "api" sånn at avhengigheten blir kopiert ut til konsumenter av denne modulen
    api(project(":sykepenger-primitiver-dto"))
    api(project(":sykepenger-utbetaling-dto"))
    api(libs.sykepenger.okonomi)
}

// Modulen var unntatt ktlint før standardiseringen, og skal ikke reformateres automatisk
ktlint {
    filter {
        exclude { true }
    }
}
