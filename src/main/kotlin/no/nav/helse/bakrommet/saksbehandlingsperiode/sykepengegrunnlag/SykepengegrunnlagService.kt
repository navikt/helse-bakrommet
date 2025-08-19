package no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag

import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeReferanse
import java.time.LocalDateTime
import java.util.*

class SykepengegrunnlagService(
    private val sykepengegrunnlagDao: SykepengegrunnlagDao,
    private val faktiskInntektDao: FaktiskInntektDao,
) {
    companion object {
        // Grunnbeløp for 2024: 124028 kroner = 12402800 øre
        // 6G for 2024: 6 * 12402800 øre = 74416800 øre (744168 kroner)
        // TODO: Hent aktuelt grunnbeløp fra ekstern kilde eller konfigurasjon
        private const val GRUNNBELØP_2024_ØRE = 12402800L // 124028 kr * 100
        private const val SEKS_G_ØRE = GRUNNBELØP_2024_ØRE * 6L // 74416800 øre
    }

    fun hentSykepengegrunnlag(referanse: SaksbehandlingsperiodeReferanse): SykepengegrunnlagResponse? {
        val grunnlag = sykepengegrunnlagDao.hentSykepengegrunnlag(referanse.periodeUUID) ?: return null
        val faktiskeInntekter = faktiskInntektDao.hentFaktiskeInntekterFor(referanse.periodeUUID)

        return grunnlag.copy(faktiskeInntekter = faktiskeInntekter)
    }

    fun opprettSykepengegrunnlag(
        referanse: SaksbehandlingsperiodeReferanse,
        request: SykepengegrunnlagRequest,
        saksbehandler: Bruker,
    ): SykepengegrunnlagResponse {
        validerSykepengegrunnlagRequest(request)

        // Lagre faktiske inntekter først
        val lagredeFaktiskeInntekter =
            request.faktiskeInntekter.map { faktiskInntekt ->
                faktiskInntektDao.opprettFaktiskInntekt(
                    faktiskInntekt.copy(opprettetAv = saksbehandler.navIdent),
                )
            }

        // Beregn sykepengegrunnlag
        val beregning = beregnSykepengegrunnlag(referanse.periodeUUID, lagredeFaktiskeInntekter, request.begrunnelse, saksbehandler)

        val lagretGrunnlag =
            sykepengegrunnlagDao.opprettSykepengegrunnlag(
                referanse.periodeUUID,
                beregning,
                saksbehandler,
            )

        return lagretGrunnlag.copy(faktiskeInntekter = lagredeFaktiskeInntekter)
    }

    fun oppdaterSykepengegrunnlag(
        referanse: SaksbehandlingsperiodeReferanse,
        request: SykepengegrunnlagRequest,
        saksbehandler: Bruker,
    ): SykepengegrunnlagResponse {
        validerSykepengegrunnlagRequest(request)

        val eksisterende =
            sykepengegrunnlagDao.hentSykepengegrunnlag(referanse.periodeUUID)
                ?: throw IllegalStateException("Finner ikke eksisterende sykepengegrunnlag for periode ${referanse.periodeUUID}")

        // Slett eksisterende faktiske inntekter
        faktiskInntektDao.slettFaktiskeInntekterFor(referanse.periodeUUID)

        // Opprett nye faktiske inntekter
        val lagredeFaktiskeInntekter =
            request.faktiskeInntekter.map { faktiskInntekt ->
                faktiskInntektDao.opprettFaktiskInntekt(
                    faktiskInntekt.copy(opprettetAv = saksbehandler.navIdent),
                )
            }

        // Beregn nytt sykepengegrunnlag
        val beregning = beregnSykepengegrunnlag(referanse.periodeUUID, lagredeFaktiskeInntekter, request.begrunnelse, saksbehandler)

        val oppdatertGrunnlag =
            sykepengegrunnlagDao.oppdaterSykepengegrunnlag(
                eksisterende.id,
                referanse.periodeUUID,
                beregning,
                saksbehandler,
                eksisterende.versjon + 1,
            )

        return oppdatertGrunnlag.copy(faktiskeInntekter = lagredeFaktiskeInntekter)
    }

    fun slettSykepengegrunnlag(
        referanse: SaksbehandlingsperiodeReferanse,
        saksbehandler: Bruker,
    ) {
        faktiskInntektDao.slettFaktiskeInntekterFor(referanse.periodeUUID)
        sykepengegrunnlagDao.slettSykepengegrunnlag(referanse.periodeUUID)
    }

    private fun validerSykepengegrunnlagRequest(request: SykepengegrunnlagRequest) {
        if (request.faktiskeInntekter.isEmpty()) {
            throw InputValideringException("Må ha minst én faktisk inntekt")
        }

        request.faktiskeInntekter.forEachIndexed { index, faktiskInntekt ->
            if (faktiskInntekt.beløpPerMånedØre < 0) {
                throw InputValideringException("Beløp per måned kan ikke være negativt (faktisk inntekt $index)")
            }

            // Valider at kilde er en gyldig enum verdi
            val gyldigeKilder = Inntektskilde.values().toSet()
            if (faktiskInntekt.kilde !in gyldigeKilder) {
                throw InputValideringException("Ugyldig kilde: ${faktiskInntekt.kilde} (faktisk inntekt $index)")
            }

            if (faktiskInntekt.erSkjønnsfastsatt && faktiskInntekt.skjønnsfastsettelseBegrunnelse.isNullOrBlank()) {
                throw InputValideringException("Skjønnsfastsettelse krever begrunnelse (faktisk inntekt $index)")
            }

            faktiskInntekt.refusjon?.let { refusjon ->
                if (refusjon.refusjonsbeløpPerMånedØre < 0) {
                    throw InputValideringException("Refusjonsbeløp kan ikke være negativt (faktisk inntekt $index)")
                }
                if (refusjon.refusjonsgrad < 0 || refusjon.refusjonsgrad > 100) {
                    throw InputValideringException("Refusjonsgrad må være mellom 0 og 100 (faktisk inntekt $index)")
                }
            }
        }
    }

    private fun beregnSykepengegrunnlag(
        saksbehandlingsperiodeId: UUID,
        faktiskeInntekter: List<FaktiskInntekt>,
        begrunnelse: String?,
        saksbehandler: Bruker,
    ): SykepengegrunnlagResponse {
        // Summer opp alle månedlige inntekter og konverter til årsinntekt (i øre)
        val totalInntektØre =
            faktiskeInntekter
                .sumOf { it.beløpPerMånedØre } * 12L
        // Månedsinntekt * 12 = årsinntekt

        // Begrens til 6G
        val begrensetTil6G = totalInntektØre > SEKS_G_ØRE
        val sykepengegrunnlagØre = if (begrensetTil6G) SEKS_G_ØRE else totalInntektØre

        return SykepengegrunnlagResponse(
            id = UUID.randomUUID(),
            // Dette settes i DAO
            saksbehandlingsperiodeId = saksbehandlingsperiodeId,
            faktiskeInntekter = faktiskeInntekter,
            totalInntektØre = totalInntektØre,
            grunnbeløp6GØre = SEKS_G_ØRE,
            begrensetTil6G = begrensetTil6G,
            sykepengegrunnlagØre = sykepengegrunnlagØre,
            begrunnelse = begrunnelse,
            opprettet = LocalDateTime.now().toString(),
            opprettetAv = saksbehandler.navIdent,
            sistOppdatert = LocalDateTime.now().toString(),
            versjon = 1,
        )
    }
}
