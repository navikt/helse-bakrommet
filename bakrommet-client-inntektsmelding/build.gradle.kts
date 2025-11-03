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
    api(project(":bakrommet-common"))
    api("no.nav.sykepenger.kontrakter:inntektsmelding-kontrakt:2025.04.04-01-56-365d3")

    testFixturesImplementation(testFixtures(project(":bakrommet-common")))
    testFixturesImplementation("io.ktor:ktor-client-mock-jvm")
    testFixturesImplementation("io.ktor:ktor-client-core")
    testFixturesImplementation("io.ktor:ktor-client-content-negotiation")
    testFixturesImplementation("io.ktor:ktor-serialization-jackson")
    testFixturesImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    testFixturesImplementation("no.nav.sykepenger.kontrakter:inntektsmelding-kontrakt:2025.04.04-01-56-365d3")
}
