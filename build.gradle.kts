plugins {
    kotlin("jvm") version "2.2.20"
    id("java-test-fixtures")
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
}

allprojects {
    group = "no.nav.helse"

    repositories {
        mavenCentral()
        maven("https://maven.pkg.github.com/navikt/*")
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }

    // Ekskluder bakrommet-dependencies fra standard konfigurasjon
    if (name != "bakrommet-dependencies") {
        apply(plugin = "org.jetbrains.kotlin.jvm")
        apply(plugin = "org.jlleitschuh.gradle.ktlint")
        apply(plugin = "java-test-fixtures")

        ktlint {
            ignoreFailures = true
            filter {
                exclude { it.file.path.contains("generated") }
                // exclude sykepenger-utbetaling modul
                exclude { it.file.path.contains("sykepenger-utbetaling") }
                exclude { it.file.path.contains("sykepenger-primitiver") }
                exclude { it.file.path.contains("sykepenger-model") }
            }
        }

        dependencies {
            constraints {
                implementation("org.apache.commons:commons-compress:1.28.0") {
                    because("org.testcontainers:postgresql:1.21.0 -> 1.24.0 har en sårbarhet")
                }
            }

            testImplementation(platform("org.junit:junit-bom:6.0.1"))
            testImplementation("org.junit.jupiter:junit-jupiter")
            testImplementation(kotlin("test"))
        }
    }
}

subprojects {
    // Ekskluder bakrommet-dependencies fra standard konfigurasjon
    if (name != "bakrommet-dependencies") {
        kotlin {
            jvmToolchain(21)
        }
        tasks {
            named<Test>("test") {
                useJUnitPlatform()
                testLogging {
                    events("skipped", "failed")
                    showStackTraces = true
                    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
                }
//                maxParallelForks =
//                    if (true) {
//                        (Runtime.getRuntime().availableProcessors() - 1).coerceAtLeast(1).coerceAtMost(4)
//                    } else {
//                        1
//                    }
            }
        }
    }
}

tasks {
    jar {
        enabled = false
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
}
