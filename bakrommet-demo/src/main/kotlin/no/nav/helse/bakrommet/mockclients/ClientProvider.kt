package no.nav.helse.bakrommet.mockclients

import no.nav.helse.bakrommet.Clienter
import no.nav.helse.bakrommet.aareg.AARegMock
import no.nav.helse.bakrommet.ainntekt.AInntektMock
import no.nav.helse.bakrommet.ereg.EregMock
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingApiMock
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingApiMock.inntektsmeldingMockHttpClient
import no.nav.helse.bakrommet.pdl.PdlMock
import no.nav.helse.bakrommet.sigrun.SigrunMock
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadBackendMock.sykepengesoknadMock
import no.nav.helse.bakrommet.testdata.Testperson

fun skapClienter(testpersoner: List<Testperson>): Clienter {
    val pdlResponses = testpersoner.associate { it.fnr to it.skapPdlReply() }
    val fnrTilSoknader = testpersoner.associate { it.fnr to it.soknader }
    val fnrÅrTilSigrunSvar: Map<Pair<String, java.time.Year>, String> =
        testpersoner
            .flatMap { testperson ->
                testperson.sigrunData.map { (year, svar) -> (testperson.fnr to year) to svar }
            }.toMap()

    val fnrTilInntektsmeldinger =
        testpersoner.filter { it.inntektsmeldinger != null }.associate { it.fnr to it.inntektsmeldinger!! }

    val fnrTilArbeidsforhold =
        testpersoner.filter { it.aaregData != null }.associate { it.fnr to it.aaregData!! }

    val clienter =
        Clienter(
            pdlClient = PdlMock.pdlClient(identTilReplyMap = pdlResponses, pdlReplyGenerator = pdlReplyGenerator),
            sykepengesoknadBackendClient = sykepengesoknadMock(fnrTilSoknader = fnrTilSoknader),
            aInntektClient = AInntektMock.aInntektClientMock(),
            aaRegClient = AARegMock.aaRegClientMock(fnrTilArbeidsforhold = fnrTilArbeidsforhold),
            eregClient = EregMock.eregClientMock(),
            inntektsmeldingClient =
                InntektsmeldingApiMock.inntektsmeldingClientMock(
                    mockClient =
                        inntektsmeldingMockHttpClient(
                            fnrTilInntektsmeldinger = fnrTilInntektsmeldinger,
                        ),
                ),
            sigrunClient = SigrunMock.sigrunMockClient(fnrÅrTilSvar = fnrÅrTilSigrunSvar),
        )
    return clienter
}

private fun Testperson.skapPdlReply(): String =
    PdlMock.pdlReply(
        fnr = this.fnr,
        aktorId = this.aktorId ?: ("${this.fnr}00"),
        fornavn = this.fornavn,
        mellomnavn = this.mellomnavn,
        etternavn = this.etternavn,
        foedselsdato = this.fødselsdato.toString(),
    )
