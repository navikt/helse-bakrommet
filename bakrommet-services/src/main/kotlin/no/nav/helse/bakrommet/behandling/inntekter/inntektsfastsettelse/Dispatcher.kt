package no.nav.helse.bakrommet.behandling.inntekter.inntektsfastsettelse

import no.nav.helse.bakrommet.clients.AInntektProvider
import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.behandling.BehandlingDbRecord
import no.nav.helse.bakrommet.behandling.dokumenter.innhenting.DokumentInnhentingDaoer
import no.nav.helse.bakrommet.behandling.inntekter.InntektData
import no.nav.helse.bakrommet.behandling.inntekter.InntektRequest
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetDao
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.Yrkesaktivitet
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingClient
import no.nav.helse.bakrommet.sigrun.SigrunClient

internal fun DokumentInnhentingDaoer.fastsettInntektData(
    request: InntektRequest,
    yrkesaktivitet: Yrkesaktivitet,
    periode: BehandlingDbRecord,
    saksbehandler: BrukerOgToken,
    yrkesaktivitetDao: YrkesaktivitetDao,
    inntektsmeldingClient: InntektsmeldingClient,
    aInntektClient: AInntektProvider,
    sigrunClient: SigrunClient,
): InntektData =
    when (request) {
        is InntektRequest.Arbeidstaker ->
            request.arbeidstakerFastsettelse(
                yrkesaktivitet = yrkesaktivitet,
                periode = periode,
                saksbehandler = saksbehandler,
                yrkesaktivitetDao = yrkesaktivitetDao,
                inntektsmeldingClient = inntektsmeldingClient,
                aInntektClient = aInntektClient,
                daoer = this,
            )

        is InntektRequest.SelvstendigNæringsdrivende ->
            request.selvstendigFastsettelse(
                periode = periode,
                saksbehandler = saksbehandler,
                sigrunClient = sigrunClient,
                daoer = this,
            )

        is InntektRequest.Inaktiv ->
            request.inaktivFastsettelse(
                periode = periode,
                saksbehandler = saksbehandler,
                sigrunClient = sigrunClient,
                daoer = this,
            )

        is InntektRequest.Frilanser ->
            request.frilanserFastsettelse(
                periode = periode,
                saksbehandler = saksbehandler,
                aInntektClient = aInntektClient,
                daoer = this,
            )

        is InntektRequest.Arbeidsledig -> request.arbeidsledigFastsettelse()
    }
