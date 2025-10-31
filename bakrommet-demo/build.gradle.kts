dependencies {
    api(project(":bakrommet-api"))
    api(project(":bakrommet-common"))

    implementation("io.ktor:ktor-server-metrics-micrometer")
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-cio")
    implementation("io.ktor:ktor-server-auth")
    implementation("io.ktor:ktor-server-auth-jwt")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-server-call-logging")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("io.ktor:ktor-serialization-jackson")

    implementation(testFixtures(project(":bakrommet-client-inntektsmelding")))
    implementation(testFixtures(project(":bakrommet-client-ainntekt")))
    implementation(testFixtures(project(":bakrommet-client-aareg")))
    implementation(testFixtures(project(":bakrommet-client-sigrun")))
    implementation(testFixtures(project(":bakrommet-client-pdl")))
}

tasks {
    val copyDeps by registering(Sync::class) {
        from(configurations.runtimeClasspath)
        exclude("bakrommet-*")
        into("build/deps")
    }
    val copyLibs by registering(Sync::class) {
        from(configurations.runtimeClasspath)
        include("bakrommet-*")
        into("build/libs")
    }

    named<Jar>("jar") {
        dependsOn(copyDeps, copyLibs)
        archiveBaseName.set("app")

        manifest {
            attributes["Main-Class"] = "no.nav.helse.bakrommet.AppKt"
            attributes["Class-Path"] =
                configurations.runtimeClasspath.get().joinToString(separator = " ") {
                    it.name
                }
        }
    }
}
