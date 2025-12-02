plugins {
    `java-test-fixtures`
}

dependencies {
    api(platform(project(":bakrommet-dependencies")))

    api("com.fasterxml.jackson.core:jackson-databind")
}
