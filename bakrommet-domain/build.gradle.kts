dependencies {
    api(platform(project(":bakrommet-dependencies")))

    implementation("com.github.navikt.spleis:sykepenger-okonomi")

    testFixturesApi(kotlin("test"))
}
