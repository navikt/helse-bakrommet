plugins {
    id("no.nav.sykepenger.kotlin")
}

// Modulen var unntatt ktlint før standardiseringen, og skal ikke reformateres automatisk
ktlint {
    filter {
        exclude { true }
    }
}
