package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeReferanse
import no.nav.helse.bakrommet.saksbehandlingsperiode.erSaksbehandlerPåSaken
import no.nav.helse.bakrommet.saksbehandlingsperiode.hentPeriode
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.beregning.beregnUtbetalingerForAlleYrkesaktiviteter
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Yrkesaktivitet
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetKategorisering
import no.nav.helse.dto.PeriodeDto
import no.nav.helse.utbetalingslinjer.Klassekode
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.UtbetalingkladdBuilder
import java.time.LocalDateTime
import java.util.*

class UtbetalingsBeregningHjelper(
    private val beregningDao: UtbetalingsberegningDao,
    private val saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao,
    private val sykepengegrunnlagDao: SykepengegrunnlagDao,
    private val yrkesaktivitetDao: YrkesaktivitetDao,
    private val personDao: PersonDao,
) {
    suspend fun settBeregning(
        referanse: SaksbehandlingsperiodeReferanse,
        saksbehandler: Bruker,
    ) {
        // Hent nødvendige data for beregningen
        val periode = saksbehandlingsperiodeDao.hentPeriode(referanse, krav = saksbehandler.erSaksbehandlerPåSaken())
        val ident =
            personDao.hentNaturligIdent(periode.spilleromPersonId)
        // Hent sykepengegrunnlag
        val sykepengegrunnlag =
            sykepengegrunnlagDao.hentSykepengegrunnlag(periode.sykepengegrunnlagId ?: return)?.sykepengegrunnlag ?: return

        // Hent inntektsforhold
        val yrkesaktiviteter = yrkesaktivitetDao.hentYrkesaktiviteter(periode)

        // Opprett input for beregning
        val beregningInput =
            UtbetalingsberegningInput(
                sykepengegrunnlag = sykepengegrunnlag,
                yrkesaktivitet = yrkesaktiviteter,
                saksbehandlingsperiode =
                    PeriodeDto(
                        fom = periode.fom,
                        tom = periode.tom,
                    ),
            )

        // Utfør beregning
        val beregnet = beregnUtbetalingerForAlleYrkesaktiviteter(beregningInput)

        // Bygg oppdrag for hver yrkesaktivitet
        val oppdrag = byggOppdragFraBeregning(beregnet, yrkesaktiviteter, ident)

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
fun byggOppdragFraBeregning(
    beregnet: List<YrkesaktivitetUtbetalingsberegning>,
    yrkesaktiviteter: List<Yrkesaktivitet>,
    ident: String,
): List<Oppdrag> {
    val oppdrag = mutableListOf<Oppdrag>()

    beregnet.forEach { yrkesaktivitetBeregning ->
        val yrkesaktivitet = yrkesaktiviteter.first { it.id == yrkesaktivitetBeregning.yrkesaktivitetId }
        val mottakerRefusjon =
            if (yrkesaktivitet.kategorisering is YrkesaktivitetKategorisering.Arbeidstaker) {
                yrkesaktivitet.kategorisering.orgnummer
            } else {
                "TODO_INGEN_REFUSJON" // Dette er en hack vi bør fikse en gang
            }

        val mottakerBruker = ident
        val klassekodeBruker = Klassekode.SykepengerArbeidstakerOrdinær

        val utbetalingkladdBuilder =
            UtbetalingkladdBuilder(
                tidslinje = yrkesaktivitetBeregning.utbetalingstidslinje,
                mottakerRefusjon = mottakerRefusjon,
                mottakerBruker = mottakerBruker,
                klassekodeBruker = klassekodeBruker,
            )

        val utbetalingkladd = utbetalingkladdBuilder.build()
        if (utbetalingkladd.arbeidsgiveroppdrag.linjer.isNotEmpty()) {
            oppdrag.add(utbetalingkladd.arbeidsgiveroppdrag)
        }
        if (utbetalingkladd.personoppdrag.linjer.isNotEmpty()) {
            oppdrag.add(utbetalingkladd.personoppdrag)
        }
    }

    return oppdrag
}
