package no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.inntektsfastsettelse

import no.nav.helse.bakrommet.ainntekt.AInntektClient
import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.inntektsmelding.InntektsmeldingClient
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.innhenting.DokumentInnhentingDaoer
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.InntektData
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.InntektRequest
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Yrkesaktivitet
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetDao
import no.nav.helse.bakrommet.sigrun.SigrunClient

internal fun DokumentInnhentingDaoer.fastsettInntektData(
    request: InntektRequest,
    yrkesaktivitet: Yrkesaktivitet,
    periode: Saksbehandlingsperiode,
    saksbehandler: BrukerOgToken,
    yrkesaktivitetDao: YrkesaktivitetDao,
    inntektsmeldingClient: InntektsmeldingClient,
    aInntektClient: AInntektClient,
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
