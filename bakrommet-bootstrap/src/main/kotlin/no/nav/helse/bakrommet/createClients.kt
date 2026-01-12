package no.nav.helse.bakrommet

import no.nav.helse.bakrommet.aareg.AARegClient
import no.nav.helse.bakrommet.ainntekt.AInntektClient
import no.nav.helse.bakrommet.auth.OboClient
import no.nav.helse.bakrommet.ereg.EregClient
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingClient
import no.nav.helse.bakrommet.pdl.PdlClient
import no.nav.helse.bakrommet.sigrun.SigrunClient
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadBackendClient

fun createClients(configuration: Configuration): Providers {
    val oboClient = OboClient(configuration.obo)
    val pdlClient = PdlClient(configuration.pdl, oboClient)
    val sykepengesoknadBackendClient =
        SykepengesoknadBackendClient(
            configuration.sykepengesoknadBackend,
            oboClient,
        )

    val aaRegClient = AARegClient(configuration.aareg, oboClient)
    val aInntektClient = AInntektClient(configuration.ainntekt, oboClient)
    val eregClient = EregClient(configuration.ereg)
    val inntektsmeldingClient = InntektsmeldingClient(configuration.inntektsmelding, oboClient)
    val sigrunClient = SigrunClient(configuration.sigrun, oboClient)

    return Providers(
        pdlClient = pdlClient,
        sykepengesoknadBackendClient = sykepengesoknadBackendClient,
        inntekterProvider = aInntektClient,
        arbeidsforholdProvider = aaRegClient,
        eregClient = eregClient,
        inntektsmeldingProvider = inntektsmeldingClient,
        sigrunClient = sigrunClient,
    )
}
