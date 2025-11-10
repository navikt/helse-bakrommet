plugins {
    `java-test-fixtures`
}

dependencies {
    api(platform(project(":bakrommet-dependencies")))

    api("com.fasterxml.jackson.core:jackson-core")
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.module:jackson-module-kotlin")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}
