private val flywayVersion = "11.5.0"
private val ktorVersion = "3.1.2"

plugins {
    kotlin("jvm") version "2.1.10"
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
    implementation("com.github.navikt:rapids-and-rivers:2025030709111741335066.dc4411f7bc29")

    implementation("org.postgresql:postgresql:42.7.5")
    implementation("com.zaxxer:HikariCP:6.2.1")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")
    implementation("com.github.seratch:kotliquery:1.9.1")
    implementation("io.micrometer:micrometer-registry-prometheus:1.14.5")
    implementation("io.ktor:ktor-client-core:$ktorVersion")

    implementation("ch.qos.logback:logback-classic:1.5.17")
    implementation("net.logstash.logback:logstash-logback-encoder:8.0") {
        exclude("com.fasterxml.jackson.core")
        exclude("com.fasterxml.jackson.dataformat")
    }

    testImplementation("com.github.navikt.tbd-libs:rapids-and-rivers-test:2025.03.10-19.50-d556269c")
    testImplementation("org.testcontainers:postgresql:1.20.6")
    testImplementation(platform("org.junit:junit-bom:5.12.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
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
