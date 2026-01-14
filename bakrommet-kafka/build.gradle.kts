plugins {
    `java-test-fixtures`
}

dependencies {
    api(platform(project(":bakrommet-dependencies")))
    implementation(project(":bakrommet-services"))

    api("org.apache.kafka:kafka-clients")
}
