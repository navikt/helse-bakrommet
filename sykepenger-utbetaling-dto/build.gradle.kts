dependencies {
    api(platform(project(":bakrommet-dependencies")))

    // bruker "implementation" fremfor "api" for å unngå
    // at avhengigheten blir transitiv, altså kopiert ut, til de som bruker denne modulen.
    implementation(project(":sykepenger-primitiver-dto"))
    implementation("com.fasterxml.jackson.core:jackson-core")
    implementation("com.github.navikt.spleis:sykepenger-okonomi")
}
