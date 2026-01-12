dependencies {
    api(project(":bakrommet-services"))
    api(project(":bakrommet-clients:bakrommet-client-aareg"))
    api(project(":bakrommet-clients:bakrommet-client-ainntekt"))
    api(project(":bakrommet-clients:bakrommet-client-inntektsmelding"))
    api(project(":bakrommet-clients:bakrommet-client-pdl"))
    api(project(":bakrommet-clients:bakrommet-client-sigrun"))
    api(project(":bakrommet-clients:bakrommet-client-sykepengesoknad"))
    api(project(":bakrommet-clients:bakrommet-client-ereg"))

    api(project(":bakrommet-db"))
    api(project(":bakrommet-api"))
    api(project(":bakrommet-kafka"))
    api(project(":bakrommet-common"))
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
