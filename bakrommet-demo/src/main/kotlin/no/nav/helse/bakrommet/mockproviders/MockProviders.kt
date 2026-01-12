package no.nav.helse.bakrommet.mockproviders

import no.nav.helse.bakrommet.Providers
import no.nav.helse.bakrommet.aareg.AARegMock
import no.nav.helse.bakrommet.ainntekt.AInntektMock
import no.nav.helse.bakrommet.ereg.EregMock
import no.nav.helse.bakrommet.infrastruktur.provider.AInntektResponse
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingApiMock
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingApiMock.inntektsmeldingMockHttpClient
import no.nav.helse.bakrommet.pdl.PdlMock
import no.nav.helse.bakrommet.sigrun.SigrunMock
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadBackendMock.sykepengesoknadMock
import no.nav.helse.bakrommet.testdata.Testperson

fun skapProviders(testpersoner: List<Testperson>): Providers {
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

    val fnrTilAinntk: Map<String, AInntektResponse> =
        testpersoner
            .filter { it.ainntektData != null }
            .associate { it.fnr to AInntektResponse(data = it.ainntektData!!) }
    val providers =
        Providers(
            personinfoProvider = PdlMock.pdlClient(identTilReplyMap = pdlResponses, pdlReplyGenerator = pdlReplyGenerator),
            sykepengesøknadProvider = sykepengesoknadMock(fnrTilSoknader = fnrTilSoknader),
            inntekterProvider = AInntektMock.aInntektClientMock(fnrTilAInntektResponse = fnrTilAinntk),
            arbeidsforholdProvider = AARegMock.aaRegClientMock(fnrTilArbeidsforhold = fnrTilArbeidsforhold),
            organisasjonsnavnProvider = EregMock.eregClientMock(),
            inntektsmeldingProvider =
                InntektsmeldingApiMock.inntektsmeldingClientMock(
                    mockClient =
                        inntektsmeldingMockHttpClient(
                            fnrTilInntektsmeldinger = fnrTilInntektsmeldinger,
                        ),
                ),
            pensjonsgivendeInntektProvider = SigrunMock.sigrunMockClient(fnrÅrTilSvar = fnrÅrTilSigrunSvar),
        )
    return providers
}

private fun Testperson.skapPdlReply(): String =
    PdlMock.pdlReply(
        fnr = this.fnr,
        aktorId = ("${this.fnr}00"),
        fornavn = this.fornavn,
        mellomnavn = this.mellomnavn,
        etternavn = this.etternavn,
        foedselsdato = this.fødselsdato.toString(),
    )
