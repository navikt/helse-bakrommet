plugins {
    id("no.nav.sykepenger.kotlin")
}

dependencies {
    api(project(":bakrommet-services"))
    api(project(":bakrommet-clients:bakrommet-client-aareg"))
    api(project(":bakrommet-clients:bakrommet-client-ainntekt"))
    api(project(":bakrommet-clients:bakrommet-client-inntektsmelding"))
    api(project(":bakrommet-clients:bakrommet-client-pdl"))
    api(project(":bakrommet-clients:bakrommet-client-sigrun"))
    api(project(":bakrommet-clients:bakrommet-client-sykepengesoknad"))
    api(project(":bakrommet-clients:bakrommet-client-ereg"))
    api(project(":bakrommet-clients:bakrommet-client-obo"))

    api(project(":bakrommet-db"))
    api(project(":bakrommet-api"))
    api(project(":bakrommet-kafka"))
}
