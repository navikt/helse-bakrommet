dependencies {
    api(project(":bakrommet-api"))
    api(project(":bakrommet-common"))

    testImplementation(testFixtures(project(":bakrommet-api")))
    testImplementation(testFixtures(project(":bakrommet-common")))
    testImplementation("no.nav.security:mock-oauth2-server:2.3.0")
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
