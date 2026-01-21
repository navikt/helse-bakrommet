package no.nav.helse.bakrommet.behandling.utbetalingsberegning

import no.nav.helse.bakrommet.behandling.BehandlingReferanse
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagBase
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagDao
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.beregning.beregnUtbetalingerForAlleYrkesaktiviteter
import no.nav.helse.bakrommet.domain.Bruker
import no.nav.helse.bakrommet.domain.person.NaturligIdent
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Behandling
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.TilkommenInntekt
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.Yrkesaktivitet
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.orgnummer
import no.nav.helse.bakrommet.kafka.dto.oppdrag.OppdragDto
import no.nav.helse.bakrommet.kafka.dto.oppdrag.SpilleromOppdragDto
import no.nav.helse.bakrommet.kafka.dto.oppdrag.UtbetalingslinjeDto
import no.nav.helse.bakrommet.repository.BehandlingRepository
import no.nav.helse.bakrommet.repository.TilkommenInntektRepository
import no.nav.helse.bakrommet.repository.VilkårsvurderingRepository
import no.nav.helse.bakrommet.repository.YrkesaktivitetRepository
import no.nav.helse.dto.PeriodeDto
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.UtbetalingkladdBuilder
import java.time.LocalDateTime
import java.util.*

class UtbetalingsBeregningHjelper(
    private val beregningDao: UtbetalingsberegningDao,
    private val sykepengegrunnlagDao: SykepengegrunnlagDao,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val tilkommenInntektRepository: TilkommenInntektRepository,
    private val yrkesaktivitetRepository: YrkesaktivitetRepository,
    private val behandlingRepository: BehandlingRepository,
) {
    fun settBeregning(
        referanse: BehandlingReferanse,
        saksbehandler: Bruker,
    ) {
        // Hent nødvendige data for beregningen
        val behandling = behandlingRepository.finn(BehandlingId(referanse.behandlingId)) ?: error("Fant ikke behandling")

        // Hent sykepengegrunnlag
        val sykepengegrunnlag =
            sykepengegrunnlagDao.finnSykepengegrunnlag(behandling.sykepengegrunnlagId ?: return)?.sykepengegrunnlag
                ?: return

        // Hent yrkesaktivitet
        val yrkesaktiviteter = yrkesaktivitetRepository.finn(behandling.id)

        val tilkommenInntekt = tilkommenInntektRepository.finnFor(behandling.id)
        // Opprett input for beregning
        val beregningInput =
            UtbetalingsberegningInput(
                sykepengegrunnlag = sykepengegrunnlag,
                yrkesaktiviteter = yrkesaktiviteter,
                tilkommenInntekt = tilkommenInntekt,
                saksbehandlingsperiode =
                    PeriodeDto(
                        fom = behandling.fom,
                        tom = behandling.tom,
                    ),
                vilkår = vilkårsvurderingRepository.hentAlle(behandling.id),
            )

        // Utfør beregning
        val beregnet = beregnUtbetalingerForAlleYrkesaktiviteter(beregningInput)

        // Bygg oppdrag for hver yrkesaktivitet
        val oppdrag = byggOppdragFraBeregning(beregnet, yrkesaktiviteter, behandling.naturligIdent)

        val spilleromUtbetalingIdViRevurderer =
            behandling.revurdererBehandlingId?.let {
                val tidligereBeregning = beregningDao.hentBeregning(it.value)
                tidligereBeregning?.beregningData?.spilleromOppdrag?.spilleromUtbetalingId
            }
        val spilleromUtbetalingId = spilleromUtbetalingIdViRevurderer ?: UUID.randomUUID().toString()
        val beregningData =
            BeregningData(
                beregnet,
                oppdrag.tilSpilleromoppdrag(fnr = behandling.naturligIdent.value, spilleromUtbetalingId = spilleromUtbetalingId),
            )

        // Opprett response
        val beregningResponse =
            BeregningResponse(
                id = UUID.randomUUID(),
                behandlingId = referanse.behandlingId,
                beregningData = beregningData,
                opprettet = LocalDateTime.now().toString(),
                opprettetAv = saksbehandler.navIdent,
                sistOppdatert = LocalDateTime.now().toString(),
            )

        beregningDao.settBeregning(
            referanse.behandlingId,
            beregningResponse,
            saksbehandler,
        )
    }

    fun beregn(
        behandling: Behandling,
        sykepengegrunnlag: SykepengegrunnlagBase,
        yrkesaktiviteter: List<Yrkesaktivitet>,
        tilkomneInntekter: List<TilkommenInntekt>,
        bruker: Bruker,
    ) {
        // Opprett input for beregning
        val beregningInput =
            UtbetalingsberegningInput(
                sykepengegrunnlag = sykepengegrunnlag,
                yrkesaktiviteter = yrkesaktiviteter,
                tilkommenInntekt = tilkomneInntekter,
                saksbehandlingsperiode =
                    PeriodeDto(
                        fom = behandling.fom,
                        tom = behandling.tom,
                    ),
                vilkår = vilkårsvurderingRepository.hentAlle(behandling.id),
            )

        // Utfør beregning
        val beregnet = beregnUtbetalingerForAlleYrkesaktiviteter(beregningInput)

        // Bygg oppdrag for hver yrkesaktivitet
        val oppdrag = byggOppdragFraBeregning(beregnet, yrkesaktiviteter, behandling.naturligIdent)

        val spilleromUtbetalingIdViRevurderer =
            behandling.revurdererBehandlingId?.let {
                val tidligereBeregning = beregningDao.hentBeregning(it.value)
                tidligereBeregning?.beregningData?.spilleromOppdrag?.spilleromUtbetalingId
            }
        val spilleromUtbetalingId = spilleromUtbetalingIdViRevurderer ?: UUID.randomUUID().toString()
        val beregningData =
            BeregningData(
                beregnet,
                oppdrag.tilSpilleromoppdrag(fnr = behandling.naturligIdent.value, spilleromUtbetalingId = spilleromUtbetalingId),
            )

        // Opprett response
        val beregningResponse =
            BeregningResponse(
                id = UUID.randomUUID(),
                behandlingId = behandling.id.value,
                beregningData = beregningData,
                opprettet = LocalDateTime.now().toString(),
                opprettetAv = bruker.navIdent,
                sistOppdatert = LocalDateTime.now().toString(),
            )

        beregningDao.settBeregning(
            behandling.id.value,
            beregningResponse,
            bruker,
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
    ident: NaturligIdent,
): List<Oppdrag> {
    val oppdrag = mutableListOf<Oppdrag>()

    beregnet.forEach { yrkesaktivitetBeregning ->
        val yrkesaktivitet = yrkesaktiviteter.first { it.id.value == yrkesaktivitetBeregning.yrkesaktivitetId }
        val mottakerRefusjon =
            if (yrkesaktivitet.kategorisering is YrkesaktivitetKategorisering.Arbeidstaker) {
                yrkesaktivitet.kategorisering.orgnummer() // TODO Hva hvis ikke orgnummer?
            } else {
                "TODO_INGEN_REFUSJON" // Dette er en hack vi bør fikse en gang
            }

        val mottakerBruker = ident.value

        val utbetalingkladdBuilder =
            UtbetalingkladdBuilder(
                tidslinje = yrkesaktivitetBeregning.utbetalingstidslinje,
                mottakerRefusjon = mottakerRefusjon,
                mottakerBruker = mottakerBruker,
                klassekodeBruker = yrkesaktivitet.kategorisering.tilKlassekode(),
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
