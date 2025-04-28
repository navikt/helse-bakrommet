private val flywayVersion = "11.5.0"
private val ktorVersion = "3.1.2"

plugins {
    kotlin("jvm") version "2.1.20"
    id("org.jlleitschuh.gradle.ktlint") version "12.2.0"
    id("com.gradleup.shadow") version "8.3.6"
}

repositories {
    val githubPassword: String by project

    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/navikt/*")
        credentials {
            username = "x-access-token"
            password = githubPassword
        }
    }
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

dependencies {
    constraints {
        implementation("org.apache.commons:commons-compress:1.27.1") {
            because("org.testcontainers:postgresql:1.21.0 -> 1.24.0 har en sårbarhet")
        }
    }

    implementation(libs.bundles.logback)
    implementation(libs.postgresJdbcDriver)
    implementation(libs.hikari)
    implementation(libs.bundles.flywayPostgres)
    implementation(libs.kotliquery)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.bundles.ktorServer)
    implementation(libs.bundles.logback)
    implementation(libs.micrometerPrometheus)

    testImplementation(libs.bundles.ktorServerTest)
    testImplementation(libs.testcontainers.postgres)
    testImplementation(libs.mock.oauth2.server)

    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    kotlin {
        jvmToolchain(21)
    }
    build {
        doLast {
            val erLokaltBygg = !System.getenv().containsKey("GITHUB_ACTION")
            val manglerPreCommitHook = !File(".git/hooks/pre-commit").isFile
            if (erLokaltBygg && manglerPreCommitHook) {
                println(
                    """
                    !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! ¯\_(⊙︿⊙)_/¯ !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                    !            Hei du! Det ser ut til at du mangler en pre-commit-hook :/         !
                    ! Du kan installere den ved å kjøre './gradlew addKtlintFormatGitPreCommitHook' !
                    !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                    """.trimIndent(),
                )
            }
        }
    }
    test {
        useJUnitPlatform()
    }

    shadowJar {
        archiveBaseName.set("app")
        archiveClassifier.set("")
        isZip64 = true
        manifest {
            attributes(
                mapOf(
                    "Main-Class" to "no.nav.helse.bakrommet.AppKt",
                ),
            )
        }
        mergeServiceFiles()
    }
}
