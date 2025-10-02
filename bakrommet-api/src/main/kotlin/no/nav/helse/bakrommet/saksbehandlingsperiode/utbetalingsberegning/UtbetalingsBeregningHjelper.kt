package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeReferanse
import no.nav.helse.bakrommet.saksbehandlingsperiode.erSaksbehandlerPåSaken
import no.nav.helse.bakrommet.saksbehandlingsperiode.hentPeriode
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetDao
import no.nav.helse.utbetalingslinjer.Klassekode
import no.nav.helse.utbetalingslinjer.UtbetalingkladdBuilder
import java.time.LocalDateTime
import java.util.*

class UtbetalingsBeregningHjelper(
    private val beregningDao: UtbetalingsberegningDao,
    private val saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao,
    private val sykepengegrunnlagDao: SykepengegrunnlagDao,
    private val yrkesaktivitetDao: YrkesaktivitetDao,
) {
    fun settBeregning(
        referanse: SaksbehandlingsperiodeReferanse,
        saksbehandler: Bruker,
    ) {
        // Hent nødvendige data for beregningen
        val periode = saksbehandlingsperiodeDao.hentPeriode(referanse, krav = saksbehandler.erSaksbehandlerPåSaken())

        // Hent sykepengegrunnlag
        val sykepengegrunnlag =
            sykepengegrunnlagDao.hentSykepengegrunnlag(referanse.periodeUUID)
                ?: return

        // Hent inntektsforhold
        val yrkesaktiviteter = yrkesaktivitetDao.hentYrkesaktivitetFor(periode)

        // Opprett input for beregning
        val beregningInput =
            UtbetalingsberegningInput(
                sykepengegrunnlag = sykepengegrunnlag,
                yrkesaktivitet = yrkesaktiviteter,
                saksbehandlingsperiode =
                    Saksbehandlingsperiode(
                        fom = periode.fom,
                        tom = periode.tom,
                    ),
            )

        // Utfør beregning
        val beregnet = UtbetalingsberegningLogikk.beregnAlaSpleis(beregningInput)

        // Bygg oppdrag for hver yrkesaktivitet
        val oppdrag = byggOppdragFraBeregning(beregnet)

        val beregningData = BeregningData(beregnet, oppdrag)

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

        beregningDao.settBeregning(
            referanse.periodeUUID,
            beregningResponse,
            saksbehandler,
        )
    }
}

/**
 * Bygger oppdrag fra en liste av yrkesaktivitet-beregninger
 */
fun byggOppdragFraBeregning(beregnet: List<YrkesaktivitetUtbetalingsberegning>): List<no.nav.helse.utbetalingslinjer.Oppdrag> {
    val oppdrag = mutableListOf<no.nav.helse.utbetalingslinjer.Oppdrag>()

    beregnet.forEach { yrkesaktivitetBeregning ->
        // TODO: Hent riktig mottaker og klassekode basert på yrkesaktivitet
        val mottakerRefusjon = "TODO" // Hent fra yrkesaktivitet
        val mottakerBruker = "TODO" // Hent fra yrkesaktivitet
        val klassekodeBruker = Klassekode.SykepengerArbeidstakerOrdinær

        val utbetalingkladdBuilder =
            UtbetalingkladdBuilder(
                tidslinje = yrkesaktivitetBeregning.utbetalingstidslinje,
                mottakerRefusjon = mottakerRefusjon,
                mottakerBruker = mottakerBruker,
                klassekodeBruker = klassekodeBruker,
            )

        val utbetalingkladd = utbetalingkladdBuilder.build()
        oppdrag.add(utbetalingkladd.arbeidsgiveroppdrag)
        oppdrag.add(utbetalingkladd.personoppdrag)
    }

    return oppdrag
}
