package no.nav.helse.bakrommet

import no.nav.helse.bakrommet.aareg.AARegMock
import no.nav.helse.bakrommet.ainntekt.AInntektMock
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingApiMock
import no.nav.helse.bakrommet.pdl.PdlMock
import no.nav.helse.bakrommet.scenarioer.Testperson
import no.nav.helse.bakrommet.sigrun.SigrunMock
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadBackendMock.sykepengesoknadMock

fun skapClienter(testpersoner: List<Testperson>): Clienter {
    val pdlResponses = testpersoner.associate { it.fnr to it.skapReply() }

    val clienter =
        Clienter(
            pdlClient = PdlMock.pdlClient(identTilReplyMap = pdlResponses),
            sykepengesoknadBackendClient = sykepengesoknadMock(),
            aInntektClient = AInntektMock.aInntektClientMock(),
            aaRegClient = AARegMock.aaRegClientMock(),
            inntektsmeldingClient = InntektsmeldingApiMock.inntektsmeldingClientMock(),
            sigrunClient = SigrunMock.sigrunMockClient(),
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
