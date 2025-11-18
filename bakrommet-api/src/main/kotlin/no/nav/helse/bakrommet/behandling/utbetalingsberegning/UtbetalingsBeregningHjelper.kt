package no.nav.helse.bakrommet.behandling.utbetalingsberegning

import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.behandling.BehandlingDao
import no.nav.helse.bakrommet.behandling.SaksbehandlingsperiodeReferanse
import no.nav.helse.bakrommet.behandling.erSaksbehandlerPåSaken
import no.nav.helse.bakrommet.behandling.hentPeriode
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagDao
import no.nav.helse.bakrommet.behandling.tilkommen.TilkommenInntektDao
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.beregning.beregnUtbetalingerForAlleYrkesaktiviteter
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetDao
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.Yrkesaktivitet
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.orgnummer
import no.nav.helse.bakrommet.kafka.dto.oppdrag.OppdragDto
import no.nav.helse.bakrommet.kafka.dto.oppdrag.SpilleromOppdragDto
import no.nav.helse.bakrommet.kafka.dto.oppdrag.UtbetalingslinjeDto
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.dto.PeriodeDto
import no.nav.helse.utbetalingslinjer.Klassekode
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.UtbetalingkladdBuilder
import java.time.LocalDateTime
import java.util.*

class UtbetalingsBeregningHjelper(
    private val beregningDao: UtbetalingsberegningDao,
    private val behandlingDao: BehandlingDao,
    private val sykepengegrunnlagDao: SykepengegrunnlagDao,
    private val yrkesaktivitetDao: YrkesaktivitetDao,
    private val personDao: PersonDao,
    private val tilkommenInntektDao: TilkommenInntektDao,
) {
    fun settBeregning(
        referanse: SaksbehandlingsperiodeReferanse,
        saksbehandler: Bruker,
    ) {
        // Hent nødvendige data for beregningen
        val periode = behandlingDao.hentPeriode(referanse, krav = saksbehandler.erSaksbehandlerPåSaken())
        val ident =
            personDao.hentNaturligIdent(periode.spilleromPersonId)
        // Hent sykepengegrunnlag
        val sykepengegrunnlag =
            sykepengegrunnlagDao.finnSykepengegrunnlag(periode.sykepengegrunnlagId ?: return)?.sykepengegrunnlag
                ?: return

        // Hent yrkesaktivitet
        val yrkesaktiviteter = yrkesaktivitetDao.hentYrkesaktiviteter(periode)

        val tilkommenInntekt = tilkommenInntektDao.hentForBehandling(periode.id)
        // Opprett input for beregning
        val beregningInput =
            UtbetalingsberegningInput(
                sykepengegrunnlag = sykepengegrunnlag,
                yrkesaktivitet = yrkesaktiviteter,
                tilkommenInntekt = tilkommenInntekt,
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

        val spilleromUtbetalingIdViRevurderer =
            periode.revurdererSaksbehandlingsperiodeId?.let {
                val tidligereBeregning = beregningDao.hentBeregning(it)
                tidligereBeregning?.beregningData?.spilleromOppdrag?.spilleromUtbetalingId
            }
        val spilleromUtbetalingId = spilleromUtbetalingIdViRevurderer ?: UUID.randomUUID().toString()
        val beregningData =
            BeregningData(
                beregnet,
                oppdrag.tilSpilleromoppdrag(fnr = ident, spilleromUtbetalingId = spilleromUtbetalingId),
            )

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

private fun List<Oppdrag>.tilSpilleromoppdrag(
    fnr: String,
    spilleromUtbetalingId: String,
): SpilleromOppdragDto =
    SpilleromOppdragDto(
        spilleromUtbetalingId = spilleromUtbetalingId,
        oppdrag = this.map { it.tilOppdragDto() },
        fnr = fnr,
    )

private fun Oppdrag.tilOppdragDto(): OppdragDto =
    OppdragDto(
        mottaker = this.mottaker,
        fagområde = this.fagområde.verdi,
        totalbeløp = this.totalbeløp(),
        linjer =
            this.linjer.map {
                UtbetalingslinjeDto(
                    fom = it.fom,
                    tom = it.tom,
                    beløp = it.beløp,
                    grad = it.grad,
                    klassekode = it.klassekode.verdi,
                    stønadsdager = it.stønadsdager(),
                )
            },
    )

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
                yrkesaktivitet.kategorisering.orgnummer() // TODO Hva hvis ikke orgnummer?
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
