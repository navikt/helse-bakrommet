package no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag

import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeReferanse
import java.time.LocalDateTime
import java.util.*

class SykepengegrunnlagService(
    private val sykepengegrunnlagDao: SykepengegrunnlagDao,
) {
    companion object {
        // Grunnbeløp for 2024: 124028 kroner = 12402800 øre
        // 6G for 2024: 6 * 12402800 øre = 74416800 øre (744168 kroner)
        // TODO: Hent aktuelt grunnbeløp fra ekstern kilde eller konfigurasjon
        private const val GRUNNBELØP_2024_ØRE = 12402800L // 124028 kr * 100
        private const val SEKS_G_ØRE = GRUNNBELØP_2024_ØRE * 6L // 74416800 øre
    }

    fun hentSykepengegrunnlag(referanse: SaksbehandlingsperiodeReferanse): SykepengegrunnlagResponse? {
        return sykepengegrunnlagDao.hentSykepengegrunnlag(referanse.periodeUUID)
    }

    fun settSykepengegrunnlag(
        referanse: SaksbehandlingsperiodeReferanse,
        request: SykepengegrunnlagRequest,
        saksbehandler: Bruker,
    ): SykepengegrunnlagResponse {
        validerSykepengegrunnlagRequest(request)

        // Beregn sykepengegrunnlag
        val beregning = beregnSykepengegrunnlag(referanse.periodeUUID, request.inntekter, request.begrunnelse, saksbehandler)

        return sykepengegrunnlagDao.settSykepengegrunnlag(
            referanse.periodeUUID,
            beregning,
            saksbehandler,
        )
    }

    fun slettSykepengegrunnlag(
        referanse: SaksbehandlingsperiodeReferanse,
        saksbehandler: Bruker,
    ) {
        sykepengegrunnlagDao.slettSykepengegrunnlag(referanse.periodeUUID)
    }

    private fun validerSykepengegrunnlagRequest(request: SykepengegrunnlagRequest) {
        if (request.inntekter.isEmpty()) {
            throw InputValideringException("Må ha minst én inntekt")
        }

        request.inntekter.forEachIndexed { index, inntekt ->
            if (inntekt.beløpPerMånedØre < 0) {
                throw InputValideringException("Beløp per måned kan ikke være negativt (inntekt $index)")
            }

            // Valider at kilde er en gyldig enum verdi
            val gyldigeKilder = Inntektskilde.values().toSet()
            if (inntekt.kilde !in gyldigeKilder) {
                throw InputValideringException("Ugyldig kilde: ${inntekt.kilde} (inntekt $index)")
            }

            if (inntekt.erSkjønnsfastsatt && inntekt.skjønnsfastsettelseBegrunnelse.isNullOrBlank()) {
                throw InputValideringException("Skjønnsfastsettelse krever begrunnelse (inntekt $index)")
            }

            inntekt.refusjon.forEachIndexed { refusjonsIndex, refusjonsperiode ->
                if (refusjonsperiode.beløpØre < 0) {
                    throw InputValideringException("Refusjonsbeløp kan ikke være negativt (inntekt $index, refusjon $refusjonsIndex)")
                }
                // Valider at fom er før eller lik tom
                if (refusjonsperiode.fom.isAfter(refusjonsperiode.tom)) {
                    throw InputValideringException("Fra-dato kan ikke være etter til-dato (inntekt $index, refusjon $refusjonsIndex)")
                }
            }
        }
    }

    private fun beregnSykepengegrunnlag(
        saksbehandlingsperiodeId: UUID,
        inntekter: List<Inntekt>,
        begrunnelse: String?,
        saksbehandler: Bruker,
    ): SykepengegrunnlagResponse {
        // Summer opp alle månedlige inntekter og konverter til årsinntekt (i øre)
        val totalInntektØre =
            inntekter
                .sumOf { it.beløpPerMånedØre } * 12L
        // Månedsinntekt * 12 = årsinntekt

        // Begrens til 6G
        val begrensetTil6G = totalInntektØre > SEKS_G_ØRE
        val sykepengegrunnlagØre = if (begrensetTil6G) SEKS_G_ØRE else totalInntektØre

        return SykepengegrunnlagResponse(
            id = UUID.randomUUID(),
            saksbehandlingsperiodeId = saksbehandlingsperiodeId,
            inntekter = inntekter,
            totalInntektØre = totalInntektØre,
            grunnbeløp6GØre = SEKS_G_ØRE,
            begrensetTil6G = begrensetTil6G,
            sykepengegrunnlagØre = sykepengegrunnlagØre,
            begrunnelse = begrunnelse,
            opprettet = LocalDateTime.now().toString(),
            opprettetAv = saksbehandler.navIdent,
            sistOppdatert = LocalDateTime.now().toString(),
        )
    }
}
