dependencies {
    // bruker "api" sånn at avhengigheten blir kopiert ut til konsumenter av denne modulen
    api(project(":sykepenger-primitiver-dto"))
    api(project(":sykepenger-utbetaling-dto"))
    api("com.github.navikt.spleis:sykepenger-okonomi")
}
