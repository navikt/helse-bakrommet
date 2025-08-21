package no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag

import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeReferanse
import no.nav.helse.bakrommet.saksbehandlingsperiode.erSaksbehandlerPåSaken
import no.nav.helse.bakrommet.saksbehandlingsperiode.hentPeriode
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold.InntektsforholdDao
import no.nav.helse.bakrommet.økonomi.Grunnbeløp
import java.time.LocalDateTime
import java.util.*

class SykepengegrunnlagService(
    private val sykepengegrunnlagDao: SykepengegrunnlagDao,
    private val inntektsforholdDao: InntektsforholdDao,
    private val saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao,
) {
    fun hentSykepengegrunnlag(referanse: SaksbehandlingsperiodeReferanse): SykepengegrunnlagResponse? {
        return sykepengegrunnlagDao.hentSykepengegrunnlag(referanse.periodeUUID)
    }

    fun settSykepengegrunnlag(
        referanse: SaksbehandlingsperiodeReferanse,
        request: SykepengegrunnlagRequest,
        saksbehandler: Bruker,
    ): SykepengegrunnlagResponse {
        validerSykepengegrunnlagRequest(request, referanse, saksbehandler)

        // Beregn sykepengegrunnlag
        val beregning = beregnSykepengegrunnlag(referanse, request.inntekter, request.begrunnelse, saksbehandler)

        return sykepengegrunnlagDao.settSykepengegrunnlag(
            referanse.periodeUUID,
            beregning,
            saksbehandler,
        )
    }

    fun slettSykepengegrunnlag(referanse: SaksbehandlingsperiodeReferanse) {
        sykepengegrunnlagDao.slettSykepengegrunnlag(referanse.periodeUUID)
    }

    private fun validerSykepengegrunnlagRequest(
        request: SykepengegrunnlagRequest,
        referanse: SaksbehandlingsperiodeReferanse,
        saksbehandler: Bruker,
    ) {
        if (request.inntekter.isEmpty()) {
            throw InputValideringException("Må ha minst én inntekt")
        }

        // Hent inntektsforhold for behandlingen
        val periode = saksbehandlingsperiodeDao.hentPeriode(referanse, krav = saksbehandler.erSaksbehandlerPåSaken())
        val inntektsforhold = inntektsforholdDao.hentInntektsforholdFor(periode)
        val inntektsforholdIds = inntektsforhold.map { it.id }.toSet()
        val requestInntektsforholdIds = request.inntekter.map { it.inntektsforholdId }.toSet()

        // Valider at alle inntekter i requesten eksisterer som inntektsforhold på behandlingen
        val manglendeInntektsforhold = requestInntektsforholdIds - inntektsforholdIds
        if (manglendeInntektsforhold.isNotEmpty()) {
            throw InputValideringException(
                "Følgende inntektsforhold finnes ikke på behandlingen: ${manglendeInntektsforhold.joinToString(", ")}",
            )
        }

        // Valider at alle inntektsforhold har inntekt i requesten
        val manglendeInntekter = inntektsforholdIds - requestInntektsforholdIds
        if (manglendeInntekter.isNotEmpty()) {
            throw InputValideringException("Følgende inntektsforhold mangler inntekt i requesten: ${manglendeInntekter.joinToString(", ")}")
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

            // Skjønnsfastsettelse er automatisk basert på kilde
            if (inntekt.kilde == Inntektskilde.SKJONNSFASTSETTELSE && request.begrunnelse.isNullOrBlank()) {
                throw InputValideringException("Skjønnsfastsettelse krever begrunnelse")
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
        referanse: SaksbehandlingsperiodeReferanse,
        inntekter: List<Inntekt>,
        begrunnelse: String?,
        saksbehandler: Bruker,
    ): SykepengegrunnlagResponse {
        // Hent perioden og skjæringstidspunkt
        val periode = saksbehandlingsperiodeDao.hentPeriode(referanse, krav = saksbehandler.erSaksbehandlerPåSaken())
        val skjæringstidspunkt =
            periode.skjæringstidspunkt
                ?: throw InputValideringException("Periode mangler skjæringstidspunkt")

        // Hent gjeldende grunnbeløp basert på skjæringstidspunkt
        val seksG = Grunnbeløp.`6G`.beløp(skjæringstidspunkt)

        // Hent virkningstidspunktet for grunnbeløpet som ble brukt
        val grunnbeløpsBeløp = seksG / 6.0 // Konverterer 6G til 1G
        val grunnbeløpVirkningstidspunkt = Grunnbeløp.virkningstidspunktFor(grunnbeløpsBeløp)

        // Summer opp alle månedlige inntekter og konverter til årsinntekt (i øre)
        val totalInntektØre = inntekter.sumOf { it.beløpPerMånedØre } * 12L

        // Begrens til 6G - konverter fra kroner til øre (1 krone = 100 øre)
        val seksGØre = (seksG.årlig * 100).toLong()
        val begrensetTil6G = totalInntektØre > seksGØre
        val sykepengegrunnlagØre = if (begrensetTil6G) seksGØre else totalInntektØre

        return SykepengegrunnlagResponse(
            id = UUID.randomUUID(),
            saksbehandlingsperiodeId = referanse.periodeUUID,
            inntekter = inntekter,
            totalInntektØre = totalInntektØre,
            grunnbeløp6GØre = seksGØre,
            begrensetTil6G = begrensetTil6G,
            sykepengegrunnlagØre = sykepengegrunnlagØre,
            begrunnelse = begrunnelse,
            grunnbeløpVirkningstidspunkt = grunnbeløpVirkningstidspunkt,
            opprettet = LocalDateTime.now().toString(),
            opprettetAv = saksbehandler.navIdent,
            sistOppdatert = LocalDateTime.now().toString(),
        )
    }
}
