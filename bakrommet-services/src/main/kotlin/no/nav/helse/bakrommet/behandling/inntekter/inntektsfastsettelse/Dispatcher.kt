package no.nav.helse.bakrommet.behandling.inntekter.inntektsfastsettelse

import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.behandling.BehandlingDbRecord
import no.nav.helse.bakrommet.behandling.dokumenter.innhenting.DokumentInnhentingDaoer
import no.nav.helse.bakrommet.behandling.inntekter.InntektData
import no.nav.helse.bakrommet.behandling.inntekter.InntektRequest
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetDao
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.LegacyYrkesaktivitet
import no.nav.helse.bakrommet.infrastruktur.provider.InntekterProvider
import no.nav.helse.bakrommet.infrastruktur.provider.InntektsmeldingProvider
import no.nav.helse.bakrommet.infrastruktur.provider.PensjonsgivendeInntektProvider

internal fun DokumentInnhentingDaoer.fastsettInntektData(
    request: InntektRequest,
    legacyYrkesaktivitet: LegacyYrkesaktivitet,
    periode: BehandlingDbRecord,
    saksbehandler: BrukerOgToken,
    yrkesaktivitetDao: YrkesaktivitetDao,
    inntektsmeldingProvider: InntektsmeldingProvider,
    inntekterProvider: InntekterProvider,
    pensjonsgivendeInntektProvider: PensjonsgivendeInntektProvider,
): InntektData =
    when (request) {
        is InntektRequest.Arbeidstaker ->
            request.arbeidstakerFastsettelse(
                legacyYrkesaktivitet = legacyYrkesaktivitet,
                periode = periode,
                saksbehandler = saksbehandler,
                yrkesaktivitetDao = yrkesaktivitetDao,
                inntektsmeldingProvider = inntektsmeldingProvider,
                inntekterProvider = inntekterProvider,
                daoer = this,
            )

        is InntektRequest.SelvstendigNæringsdrivende ->
            request.selvstendigFastsettelse(
                periode = periode,
                saksbehandler = saksbehandler,
                pensjonsgivendeInntektProvider = pensjonsgivendeInntektProvider,
                daoer = this,
            )

        is InntektRequest.Inaktiv ->
            request.inaktivFastsettelse(
                periode = periode,
                saksbehandler = saksbehandler,
                pensjonsgivendeInntektProvider = pensjonsgivendeInntektProvider,
                daoer = this,
            )

        is InntektRequest.Frilanser ->
            request.frilanserFastsettelse(
                periode = periode,
                saksbehandler = saksbehandler,
                inntekterProvider = inntekterProvider,
                daoer = this,
            )

        is InntektRequest.Arbeidsledig -> request.arbeidsledigFastsettelse()
    }
