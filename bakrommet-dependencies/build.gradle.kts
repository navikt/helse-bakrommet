plugins {
    `java-platform`
}

group = "no.nav.helse"

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/navikt/*")
        credentials {
            username = "x-access-token"
            password = project.findProperty("githubPassword") as String? ?: ""
        }
    }
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

javaPlatform {
    allowDependencies() // vi kan importere BOMs
}

dependencies {
    // BOMs for å sikre samsvar
    api(platform("com.fasterxml.jackson:jackson-bom:2.18.2"))
    api(platform("io.ktor:ktor-bom:3.3.1"))
    
    constraints {
        // Kotlin
        api("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
        api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

        // Logging
        api("ch.qos.logback:logback-classic:1.5.18")
        api("org.slf4j:slf4j-api:2.0.17")
        api("net.logstash.logback:logstash-logback-encoder:8.1")

        // Database
        api("org.postgresql:postgresql:42.7.8")
        api("com.zaxxer:HikariCP:6.3.0")
        api("org.flywaydb:flyway-core:11.13.2")
        api("org.flywaydb:flyway-database-postgresql:11.13.2")
        api("com.github.seratch:kotliquery:1.9.1")

        // Kafka
        api("org.apache.kafka:kafka-clients:3.8.0")

        // Monitoring
        api("io.micrometer:micrometer-registry-prometheus:1.15.4")

        // Testing
        api("org.testcontainers:postgresql:1.21.3")
        api("no.nav.security:mock-oauth2-server:3.0.0")

        // NAV dependencies
        api("no.nav.helse.flex:sykepengesoknad-kafka:2025.09.23-17.33-5e6b02b6")
        api("com.github.navikt.spleis:sykepenger-okonomi:2025.09.05-14.51-15db36a7")
    }
}
