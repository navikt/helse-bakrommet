package no.nav.helse.bakrommet.saksbehandlingsperiode.beregning

import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeReferanse
import no.nav.helse.bakrommet.saksbehandlingsperiode.erSaksbehandlerPåSaken
import no.nav.helse.bakrommet.saksbehandlingsperiode.hentPeriode
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold.InntektsforholdDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagDao
import java.time.LocalDateTime
import java.util.*

class BeregningService(
    private val beregningDao: BeregningDao,
    private val saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao,
    private val sykepengegrunnlagDao: SykepengegrunnlagDao,
    private val inntektsforholdDao: InntektsforholdDao,
) {
    fun hentBeregning(referanse: SaksbehandlingsperiodeReferanse): BeregningResponse? {
        return beregningDao.hentBeregning(referanse.periodeUUID)
    }

    fun settBeregning(
        referanse: SaksbehandlingsperiodeReferanse,
        saksbehandler: Bruker,
    ): BeregningResponse {
        // Hent nødvendige data for beregningen
        val periode = saksbehandlingsperiodeDao.hentPeriode(referanse, krav = saksbehandler.erSaksbehandlerPåSaken())

        // Hent sykepengegrunnlag
        val sykepengegrunnlag =
            sykepengegrunnlagDao.hentSykepengegrunnlag(referanse.periodeUUID)
                ?: throw InputValideringException("Mangler sykepengegrunnlag for perioden")

        // Hent inntektsforhold
        val inntektsforhold = inntektsforholdDao.hentInntektsforholdFor(periode)

        // Opprett input for beregning
        val beregningInput =
            BeregningInput(
                sykepengegrunnlag = sykepengegrunnlag,
                inntektsforhold = inntektsforhold,
            )

        // Utfør beregning
        val beregningData = BeregningLogikk.beregn(beregningInput)

        // Opprett response
        val beregningResponse =
            BeregningResponse(
                id = UUID.randomUUID(),
                saksbehandlingsperiodeId = referanse.periodeUUID,
                beregningData = beregningData,
                opprettet = LocalDateTime.now().toString(),
                opprettetAv = saksbehandler.navIdent,
                sistOppdatert = LocalDateTime.now().toString(),
            )

        return beregningDao.settBeregning(
            referanse.periodeUUID,
            beregningResponse,
            saksbehandler,
        )
    }

    fun slettBeregning(referanse: SaksbehandlingsperiodeReferanse) {
        beregningDao.slettBeregning(referanse.periodeUUID)
    }
}
