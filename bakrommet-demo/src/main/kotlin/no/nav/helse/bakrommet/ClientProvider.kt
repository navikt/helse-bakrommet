package no.nav.helse.bakrommet

import no.nav.helse.bakrommet.aareg.AARegMock
import no.nav.helse.bakrommet.ainntekt.AInntektMock
import no.nav.helse.bakrommet.auth.OAuthScope
import no.nav.helse.bakrommet.auth.OboClient
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingApiMock
import no.nav.helse.bakrommet.pdl.PdlMock
import no.nav.helse.bakrommet.scenarioer.Testperson
import no.nav.helse.bakrommet.sigrun.SigrunMock
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadBackendClient

fun skapClienter(testpersoner: List<Testperson>): Clienter {
    val pdlResponses = testpersoner.associate { it.fnr to it.skapReply() }

    // Sett opp mock-klienter tilsvarende e2e-testene
    val oboClient: OboClient = PdlMock.createDefaultOboClient()

    val pdlClient = PdlMock.pdlClient(identTilReplyMap = pdlResponses)
    val aaRegClient = AARegMock.aaRegClientMock(oboClient = oboClient)
    val aInntektClient = AInntektMock.aInntektClientMock(oboClient = oboClient)
    val sigrunClient = SigrunMock.sigrunMockClient(oboClient = oboClient)
    val inntektsmeldingClient = InntektsmeldingApiMock.inntektsmeldingClientMock(oboClient = oboClient)

    // Enkel mock av sykepengesøknad-backend-klienten for demoformål
    val sykepengesoknadBackendClient =
        SykepengesoknadBackendClient(
            configuration =
                Configuration.SykepengesoknadBackend(
                    hostname = "sykepengesoknad-backend",
                    scope = OAuthScope("sykepengesoknad-backend-scope"),
                ),
            oboClient = oboClient,
        )

    val clienter =
        Clienter(
            pdlClient = pdlClient,
            sykepengesoknadBackendClient = sykepengesoknadBackendClient,
            aInntektClient = aInntektClient,
            aaRegClient = aaRegClient,
            inntektsmeldingClient = inntektsmeldingClient,
            sigrunClient = sigrunClient,
        )
    return clienter
}

private fun Testperson.skapReply(): String =
    PdlMock.pdlReply(
        fnr = this.fnr,
        aktorId = this.aktorId ?: ("${this.fnr}00"),
        fornavn = this.fornavn,
        mellomnavn = this.mellomnavn,
        etternavn = this.etternavn,
        foedselsdato = this.fødselsdato.toString(),
    )
