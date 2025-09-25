val commonsCodecVersion = "1.15"

dependencies {
    api(platform(project(":bakrommet-dependencies")))
    api("commons-codec:commons-codec:$commonsCodecVersion")

    // bruker "api" sånn at avhengigheten blir kopiert ut til konsumenter av denne modulen
    api(project(":sykepenger-utbetaling-dto"))

    implementation(project(":sykepenger-primitiver"))
    implementation("com.github.navikt.spleis:sykepenger-okonomi")
    testFixturesImplementation("com.github.navikt.spleis:sykepenger-okonomi")
    testFixturesImplementation(project(":sykepenger-primitiver"))
    implementation("org.slf4j:slf4j-api")
}
