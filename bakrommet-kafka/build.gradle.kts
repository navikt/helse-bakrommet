plugins {
    `java-test-fixtures`
}

dependencies {
    implementation(project(":bakrommet-services"))

    api("org.apache.kafka:kafka-clients")
}
