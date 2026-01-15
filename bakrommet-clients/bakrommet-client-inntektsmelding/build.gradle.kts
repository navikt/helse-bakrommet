plugins {
    `java-test-fixtures`
}

repositories {
    maven {
        url = uri("https://maven.pkg.github.com/navikt/*")
        credentials {
            username = "x-access-token"
            password = project.findProperty("githubPassword") as String? ?: ""
        }
    }
}

dependencies {
    api(platform(project(":bakrommet-dependencies")))
    api(project(":bakrommet-clients:bakrommet-client-common"))

    testFixturesImplementation("no.nav.sykepenger.kontrakter:inntektsmelding-kontrakt:2025.04.04-01-56-365d3")

    testFixturesImplementation(project(":bakrommet-clients:bakrommet-client-common"))
    testFixturesImplementation(testFixtures(project(":bakrommet-clients:bakrommet-client-common")))
}
