package no.nav.helse.bakrommet.behandling.inntekter.inntektsfastsettelse

import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Behandling
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.InntektData
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.InntektRequest
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.Yrkesaktivitetsperiode
import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer
import no.nav.helse.bakrommet.infrastruktur.provider.InntekterProvider
import no.nav.helse.bakrommet.infrastruktur.provider.InntektsmeldingProvider
import no.nav.helse.bakrommet.infrastruktur.provider.PensjonsgivendeInntektProvider

internal fun AlleDaoer.fastsettInntektData(
    request: InntektRequest,
    yrkesaktivitetsperiode: Yrkesaktivitetsperiode,
    periode: Behandling,
    saksbehandler: BrukerOgToken,
    inntektsmeldingProvider: InntektsmeldingProvider,
    inntekterProvider: InntekterProvider,
    pensjonsgivendeInntektProvider: PensjonsgivendeInntektProvider,
): InntektData =
    when (request) {
        is InntektRequest.Arbeidstaker -> {
            request.arbeidstakerFastsettelse(
                yrkesaktivitetsperiode = yrkesaktivitetsperiode,
                behandling = periode,
                saksbehandler = saksbehandler,
                inntektsmeldingProvider = inntektsmeldingProvider,
                inntekterProvider = inntekterProvider,
                daoer = this,
            )
        }

        is InntektRequest.SelvstendigNæringsdrivende -> {
            request.selvstendigFastsettelse(
                behandling = periode,
                saksbehandler = saksbehandler,
                pensjonsgivendeInntektProvider = pensjonsgivendeInntektProvider,
                daoer = this,
            )
        }

        is InntektRequest.Inaktiv -> {
            request.inaktivFastsettelse(
                behandling = periode,
                saksbehandler = saksbehandler,
                pensjonsgivendeInntektProvider = pensjonsgivendeInntektProvider,
                daoer = this,
            )
        }

        is InntektRequest.Frilanser -> {
            request.frilanserFastsettelse(
                behandling = periode,
                saksbehandler = saksbehandler,
                inntekterProvider = inntekterProvider,
                daoer = this,
            )
        }

        is InntektRequest.Arbeidsledig -> {
            request.arbeidsledigFastsettelse()
        }
    }
