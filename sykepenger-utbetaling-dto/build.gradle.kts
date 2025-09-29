dependencies {
    api(platform(project(":bakrommet-dependencies")))

    // bruker "implementation" fremfor "api" for å unngå
    // at avhengigheten blir transitiv, altså kopiert ut, til de som bruker denne modulen.
    implementation(project(":sykepenger-primitiver-dto"))
    implementation("com.fasterxml.jackson.core:jackson-core")
    implementation("com.fasterxml.jackson.core:jackson-annotations")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.github.navikt.spleis:sykepenger-okonomi")
}
