plugins {
    `java-test-fixtures`
}

dependencies {
    api(project(":bakrommet-common"))

    testFixturesImplementation(testFixtures(project(":bakrommet-common")))
}
