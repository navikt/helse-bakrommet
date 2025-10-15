package no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag

import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.infrastruktur.db.TransactionalSessionFactory
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.*
// import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.UtbetalingsBeregningHjelper
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.UtbetalingsberegningDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetDbRecord
import no.nav.helse.bakrommet.økonomi.Grunnbeløp
import no.nav.helse.dto.InntektbeløpDto
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
            val dbRecord = sykepengegrunnlagDao.hentSykepengegrunnlag(referanse.periodeUUID)
            dbRecord?.let { konverterTilResponse(it, referanse.periodeUUID) }
        }

    fun settSykepengegrunnlag(
        referanse: SaksbehandlingsperiodeReferanse,
        request: SykepengegrunnlagRequest,
        saksbehandler: Bruker,
    ): SykepengegrunnlagResponse =
        db.transactional {
            val periode =
                saksbehandlingsperiodeDao.hentPeriode(referanse, krav = saksbehandler.erSaksbehandlerPåSaken())
            val yrkesaktiviteter = yrkesaktivitetDao.hentYrkesaktivitetFor(periode)
            validerSykepengegrunnlagRequest(this, request, referanse, saksbehandler, yrkesaktiviteter)

            val beregning =
                beregnSykepengegrunnlag(
                    periode,
                    request.inntekter,
                    request.begrunnelse,
                    saksbehandler,
                    yrkesaktiviteter,
                )
            // Sett sykepengegrunnlag og beregning i samme transaksjon
            val sykepengegrunnlagFraBeregning = konverterFraResponse(beregning, saksbehandler)
            val dbRecord = sykepengegrunnlagDao.lagreSykepengegrunnlag(sykepengegrunnlagFraBeregning, saksbehandler)
            val sykepengegrunnlagResponse = konverterTilResponse(dbRecord, referanse.periodeUUID)

            // TODO: Oppdater beregning basert på det nye sykepengegrunnlaget
            // Dette krever at UtbetalingsBeregningHjelper oppdateres til å bruke den nye DAO-en
            // val beregningshjelperISammeTransaksjon =
            //     UtbetalingsBeregningHjelper(
            //         beregningDao,
            //         saksbehandlingsperiodeDao,
            //         sykepengegrunnlagDao,
            //         yrkesaktivitetDao,
            //         personDao,
            //     )
            // beregningshjelperISammeTransaksjon.settBeregning(referanse, saksbehandler)
            sykepengegrunnlagResponse
        }

    fun slettSykepengegrunnlag(referanse: SaksbehandlingsperiodeReferanse) =
        db.transactional {
            sykepengegrunnlagDao.slettSykepengegrunnlag(referanse.periodeUUID)
            beregningDao.slettBeregning(referanse.periodeUUID)
        }

    private fun validerSykepengegrunnlagRequest(
        daoer: SykepengegrunnlagServiceDaoer,
        request: SykepengegrunnlagRequest,
        referanse: SaksbehandlingsperiodeReferanse,
        saksbehandler: Bruker,
        yrkesaktiviteter: List<YrkesaktivitetDbRecord>,
    ) {
        if (request.inntekter.isEmpty()) {
            throw InputValideringException("Må ha minst én inntekt")
        }

        // Hent inntektsforhold for behandlingen
        val periode =
            daoer.saksbehandlingsperiodeDao.hentPeriode(referanse, krav = saksbehandler.erSaksbehandlerPåSaken())
        val yrkesaktivitetIds = yrkesaktiviteter.map { it.id }.toSet()
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
        yrkesaktiviteter: List<YrkesaktivitetDbRecord>,
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
        // Begrens til 6G - konverter fra kroner til øre (1 krone = 100 øre)
        val seksGØre = (seksG.årlig * 100).toLong()

        // Summer opp alle inntekter som kommer fra arbeidstaker ved å se om yrkesaktivteten er arbeidstaker

        val sumAvArbeidstakerInntekterØre =
            inntekter
                .filter { inntekt ->
                    val yrkesaktivitet = yrkesaktiviteter.first { it.id == inntekt.yrkesaktivitetId }
                    yrkesaktivitet.kategorisering["INNTEKTSKATEGORI"] == "ARBEIDSTAKER"
                }.sumOf { it.beløpPerMånedØre }

        val inntekterBeregnet =
            inntekter.map { inntekt ->

                val yrkesaktivitet = yrkesaktiviteter.first { it.id == inntekt.yrkesaktivitetId }
                yrkesaktivitet.kategorisering["INNTEKTSKATEGORI"] == "ARBEIDSTAKER"

                fun finnGrunnlag(): Long {
                    if (yrkesaktivitet.kategorisering["INNTEKTSKATEGORI"] == "SELVSTENDIG_NÆRINGSDRIVENDE") {
                        val pensjonsgivendeCappet6g = inntekt.beløpPerMånedØre.coerceAtMost(seksGØre)
                        val næringsinntekt = pensjonsgivendeCappet6g - sumAvArbeidstakerInntekterØre
                        // returner næringsinntekt hvis større enn 0, eller snull
                        return næringsinntekt.coerceAtLeast(0L)
                    } else {
                        return inntekt.beløpPerMånedØre
                    }
                }
                InntektBeregnet(
                    yrkesaktivitetId = inntekt.yrkesaktivitetId,
                    inntektMånedligØre = inntekt.beløpPerMånedØre,
                    grunnlagMånedligØre = finnGrunnlag(),
                    kilde = inntekt.kilde,
                    refusjon = inntekt.refusjon,
                )
            }

        // Summer opp alle månedlige inntekter og konverter til årsinntekt (i øre)
        val totalInntektØre = inntekterBeregnet.sumOf { it.grunnlagMånedligØre } * 12L

        val begrensetTil6G = totalInntektØre > seksGØre
        val sykepengegrunnlagØre = if (begrensetTil6G) seksGØre else totalInntektØre

        return SykepengegrunnlagResponse(
            id = UUID.randomUUID(),
            saksbehandlingsperiodeId = periode.id,
            inntekter = inntekterBeregnet,
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

    private fun konverterFraResponse(
        response: SykepengegrunnlagResponse,
        saksbehandler: Bruker,
    ): Sykepengegrunnlag =
        Sykepengegrunnlag(
            grunnbeløp = InntektbeløpDto.Årlig(response.grunnbeløpØre / 100.0), // Konverter fra øre til kroner
            sykepengegrunnlag = InntektbeløpDto.Årlig(response.sykepengegrunnlagØre / 100.0),
            seksG = InntektbeløpDto.Årlig(response.grunnbeløp6GØre / 100.0),
            begrensetTil6G = response.begrensetTil6G,
            grunnbeløpVirkningstidspunkt = response.grunnbeløpVirkningstidspunkt,
            opprettet = response.opprettet,
            opprettetAv = response.opprettetAv,
        )

    private fun konverterTilResponse(
        dbRecord: SykepengegrunnlagDbRecord,
        saksbehandlingsperiodeId: UUID,
    ): SykepengegrunnlagResponse {
        val sykepengegrunnlag = dbRecord.sykepengegrunnlag
        return SykepengegrunnlagResponse(
            id = dbRecord.id,
            saksbehandlingsperiodeId = saksbehandlingsperiodeId,
            inntekter = emptyList(), // TODO: Implementer inntekter hvis nødvendig
            totalInntektØre = (sykepengegrunnlag.sykepengegrunnlag.beløp * 100).toLong(), // Konverter fra kroner til øre
            grunnbeløpØre = (sykepengegrunnlag.grunnbeløp.beløp * 100).toLong(),
            grunnbeløp6GØre = (sykepengegrunnlag.seksG.beløp * 100).toLong(),
            begrensetTil6G = sykepengegrunnlag.begrensetTil6G,
            sykepengegrunnlagØre = (sykepengegrunnlag.sykepengegrunnlag.beløp * 100).toLong(),
            begrunnelse = null, // TODO: Legg til begrunnelse hvis nødvendig
            grunnbeløpVirkningstidspunkt = sykepengegrunnlag.grunnbeløpVirkningstidspunkt,
            opprettet = sykepengegrunnlag.opprettet,
            opprettetAv = sykepengegrunnlag.opprettetAv,
            sistOppdatert = dbRecord.oppdatert.toString(),
        )
    }
}
