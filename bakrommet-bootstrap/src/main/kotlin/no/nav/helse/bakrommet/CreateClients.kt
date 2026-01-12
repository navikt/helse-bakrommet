package no.nav.helse.bakrommet

import no.nav.helse.bakrommet.aareg.AARegClient
import no.nav.helse.bakrommet.ainntekt.AInntektClient
import no.nav.helse.bakrommet.auth.OboClient
import no.nav.helse.bakrommet.clients.AARegProvider
import no.nav.helse.bakrommet.clients.AInntektProvider
import no.nav.helse.bakrommet.clients.EregProvider
import no.nav.helse.bakrommet.clients.InntektsmeldingProvider
import no.nav.helse.bakrommet.clients.PdlProvider
import no.nav.helse.bakrommet.clients.SigrunProvider
import no.nav.helse.bakrommet.clients.SykepengesoknadBackendProvider
import no.nav.helse.bakrommet.ereg.EregClient
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingClient
import no.nav.helse.bakrommet.pdl.PdlClient
import no.nav.helse.bakrommet.sigrun.SigrunClient
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadBackendClient

class Clienter(
    val pdlClient: PdlProvider,
    val sykepengesoknadBackendClient: SykepengesoknadBackendProvider,
    val aInntektClient: AInntektProvider,
    val aaRegClient: AARegProvider,
    val eregClient: EregProvider,
    val inntektsmeldingClient: InntektsmeldingProvider,
    val sigrunClient: SigrunProvider,
)

fun createClients(configuration: Configuration): Clienter {
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

    return Clienter(
        pdlClient = pdlClient,
        sykepengesoknadBackendClient = sykepengesoknadBackendClient,
        aInntektClient = aInntektClient,
        aaRegClient = aaRegClient,
        eregClient = eregClient,
        inntektsmeldingClient = inntektsmeldingClient,
        sigrunClient = sigrunClient,
    )
}
