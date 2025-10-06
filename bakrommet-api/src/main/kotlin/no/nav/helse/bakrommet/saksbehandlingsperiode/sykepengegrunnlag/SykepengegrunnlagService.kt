package no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag

import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.infrastruktur.db.TransactionalSessionFactory
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.*
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.UtbetalingsBeregningHjelper
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.UtbetalingsberegningDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetDao
import no.nav.helse.bakrommet.økonomi.Grunnbeløp
import java.time.LocalDateTime
import java.util.*

interface SykepengegrunnlagServiceDaoer {
    val sykepengegrunnlagDao: SykepengegrunnlagDao
    val saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao
    val yrkesaktivitetDao: YrkesaktivitetDao
    val beregningDao: UtbetalingsberegningDao
    val personDao: PersonDao
}

class SykepengegrunnlagService(
    daoer: SykepengegrunnlagServiceDaoer,
    sessionFactory: TransactionalSessionFactory<SykepengegrunnlagServiceDaoer>,
) {
    private val db = DbDaoer(daoer, sessionFactory)

    fun hentSykepengegrunnlag(referanse: SaksbehandlingsperiodeReferanse): SykepengegrunnlagResponse? =
        db.nonTransactional {
            sykepengegrunnlagDao.hentSykepengegrunnlag(referanse.periodeUUID)
        }

    fun settSykepengegrunnlag(
        referanse: SaksbehandlingsperiodeReferanse,
        request: SykepengegrunnlagRequest,
        saksbehandler: Bruker,
    ): SykepengegrunnlagResponse {
        return db.transactional {
            validerSykepengegrunnlagRequest(this, request, referanse, saksbehandler)
            val periode =
                saksbehandlingsperiodeDao.hentPeriode(referanse, krav = saksbehandler.erSaksbehandlerPåSaken())

            val beregning = beregnSykepengegrunnlag(periode, request.inntekter, request.begrunnelse, saksbehandler)
            // Sett sykepengegrunnlag og beregning i samme transaksjon
            val sykepengegrunnlagResponse =
                sykepengegrunnlagDao.settSykepengegrunnlag(
                    referanse.periodeUUID,
                    beregning,
                    saksbehandler,
                )

            // Oppdater beregning basert på det nye sykepengegrunnlaget
            val beregningshjelperISammeTransaksjon =
                UtbetalingsBeregningHjelper(
                    beregningDao,
                    saksbehandlingsperiodeDao,
                    sykepengegrunnlagDao,
                    yrkesaktivitetDao,
                    personDao,
                )
            beregningshjelperISammeTransaksjon.settBeregning(referanse, saksbehandler)
            sykepengegrunnlagResponse
        }
    }

    fun slettSykepengegrunnlag(referanse: SaksbehandlingsperiodeReferanse) {
        return db.transactional {
            sykepengegrunnlagDao.slettSykepengegrunnlag(referanse.periodeUUID)
            beregningDao.slettBeregning(referanse.periodeUUID)
        }
    }

    private fun validerSykepengegrunnlagRequest(
        daoer: SykepengegrunnlagServiceDaoer,
        request: SykepengegrunnlagRequest,
        referanse: SaksbehandlingsperiodeReferanse,
        saksbehandler: Bruker,
    ) {
        if (request.inntekter.isEmpty()) {
            throw InputValideringException("Må ha minst én inntekt")
        }

        // Hent inntektsforhold for behandlingen
        val periode =
            daoer.saksbehandlingsperiodeDao.hentPeriode(referanse, krav = saksbehandler.erSaksbehandlerPåSaken())
        val inntektsforhold = daoer.yrkesaktivitetDao.hentYrkesaktivitetFor(periode)
        val yrkesaktivitetIds = inntektsforhold.map { it.id }.toSet()
        val requestYrkesaktivitetIds = request.inntekter.map { it.yrkesaktivitetId }.toSet()

        // Valider at alle inntekter i requesten eksisterer som inntektsforhold på behandlingen
        val manglendeYrkesaktivitet = requestYrkesaktivitetIds - yrkesaktivitetIds
        if (manglendeYrkesaktivitet.isNotEmpty()) {
            throw InputValideringException(
                "Følgende inntektsforhold finnes ikke på behandlingen: ${manglendeYrkesaktivitet.joinToString(", ")}",
            )
        }

        // Valider at alle inntektsforhold har inntekt i requesten
        val manglendeInntekter = yrkesaktivitetIds - requestYrkesaktivitetIds
        if (manglendeInntekter.isNotEmpty()) {
            throw InputValideringException(
                "Følgende inntektsforhold mangler inntekt i requesten: ${
                    manglendeInntekter.joinToString(
                        ", ",
                    )
                }",
            )
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
                // Valider at fom er før eller lik tom (hvis tom er satt)
                if (refusjonsperiode.tom != null && refusjonsperiode.fom.isAfter(refusjonsperiode.tom)) {
                    throw InputValideringException("Fra-dato kan ikke være etter til-dato (inntekt $index, refusjon $refusjonsIndex)")
                }
            }
        }
    }

    private fun beregnSykepengegrunnlag(
        periode: Saksbehandlingsperiode,
        inntekter: List<Inntekt>,
        begrunnelse: String?,
        saksbehandler: Bruker,
    ): SykepengegrunnlagResponse {
        // Hent perioden og skjæringstidspunkt
        val skjæringstidspunkt =
            periode.skjæringstidspunkt
                ?: throw InputValideringException("Periode mangler skjæringstidspunkt")

        // Hent gjeldende grunnbeløp basert på skjæringstidspunkt
        val seksG = Grunnbeløp.`6G`.beløp(skjæringstidspunkt)

        // Hent virkningstidspunktet for grunnbeløpet som ble brukt
        val grunnbeløpsBeløp = seksG / 6.0 // Konverterer 6G til 1G
        val grunnbeløpVirkningstidspunkt = Grunnbeløp.virkningstidspunktFor(grunnbeløpsBeløp)

        // Beregn 1G i øre
        val grunnbeløpØre = (grunnbeløpsBeløp.årlig * 100).toLong()

        // Summer opp alle månedlige inntekter og konverter til årsinntekt (i øre)
        val totalInntektØre = inntekter.sumOf { it.beløpPerMånedØre } * 12L

        // Begrens til 6G - konverter fra kroner til øre (1 krone = 100 øre)
        val seksGØre = (seksG.årlig * 100).toLong()
        val begrensetTil6G = totalInntektØre > seksGØre
        val sykepengegrunnlagØre = if (begrensetTil6G) seksGØre else totalInntektØre

        return SykepengegrunnlagResponse(
            id = UUID.randomUUID(),
            saksbehandlingsperiodeId = periode.id,
            inntekter = inntekter,
            totalInntektØre = totalInntektØre,
            grunnbeløpØre = grunnbeløpØre,
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
