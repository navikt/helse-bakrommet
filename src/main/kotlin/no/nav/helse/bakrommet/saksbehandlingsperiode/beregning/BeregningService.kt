package no.nav.helse.bakrommet.saksbehandlingsperiode.beregning

import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeReferanse
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dagtype
import no.nav.helse.bakrommet.saksbehandlingsperiode.erSaksbehandlerPåSaken
import no.nav.helse.bakrommet.saksbehandlingsperiode.hentPeriode
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagDao
import java.time.LocalDateTime
import java.util.*

class BeregningService(
    private val beregningDao: BeregningDao,
    private val saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao,
    private val sykepengegrunnlagDao: SykepengegrunnlagDao,
) {
    fun hentBeregning(referanse: SaksbehandlingsperiodeReferanse): BeregningResponse? {
        return beregningDao.hentBeregning(referanse.periodeUUID)
    }

    fun settBeregning(
        referanse: SaksbehandlingsperiodeReferanse,
        request: BeregningRequest,
        saksbehandler: Bruker,
    ): BeregningResponse {
        validerBeregningRequest(request, referanse, saksbehandler)

        // Hent nødvendige data for beregningen
        val periode = saksbehandlingsperiodeDao.hentPeriode(referanse, krav = saksbehandler.erSaksbehandlerPåSaken())

        // Hent sykepengegrunnlag
        val sykepengegrunnlag =
            sykepengegrunnlagDao.hentSykepengegrunnlag(referanse.periodeUUID)
                ?: throw InputValideringException("Mangler sykepengegrunnlag for perioden")

        // Konverter dagoversikt til riktig format
        val dagoversikt =
            request.dagoversikt.map { dag ->
                DagoversiktDag(
                    dato = dag.dato,
                    dagtype = dag.dagtype.name,
                    grad = dag.grad,
                    yrkesaktivitetId = dag.yrkesaktivitetId,
                )
            }

        // Opprett input for beregning
        val beregningInput =
            BeregningInput(
                dagoversikt = dagoversikt,
                sykepengegrunnlag =
                    SykepengegrunnlagInput(
                        sykepengegrunnlagØre = sykepengegrunnlag.sykepengegrunnlagØre,
                    ),
                refusjon = request.refusjon,
                maksdao = request.maksdao,
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

    private fun validerBeregningRequest(
        request: BeregningRequest,
        referanse: SaksbehandlingsperiodeReferanse,
        saksbehandler: Bruker,
    ) {
        if (request.dagoversikt.isEmpty()) {
            throw InputValideringException("Må ha minst én dag i dagoversikt")
        }

        if (request.maksdao <= 0) {
            throw InputValideringException("Maksdao må være større enn 0")
        }

        // Valider at alle dager har gyldig dagtype
        request.dagoversikt.forEachIndexed { index, dag ->
            try {
                Dagtype.valueOf(dag.dagtype.name)
            } catch (e: IllegalArgumentException) {
                throw InputValideringException("Ugyldig dagtype: ${dag.dagtype} (dag $index)")
            }

            // Valider grad
            dag.grad?.let { grad ->
                if (grad < 0 || grad > 100) {
                    throw InputValideringException("Grad må være mellom 0 og 100 (dag $index)")
                }
            }
        }

        // Valider refusjon
        request.refusjon.forEachIndexed { index, refusjon ->
            if (refusjon.beløpØre < 0) {
                throw InputValideringException("Refusjonsbeløp kan ikke være negativt (refusjon $index)")
            }
            if (refusjon.fom.isAfter(refusjon.tom)) {
                throw InputValideringException("Fra-dato kan ikke være etter til-dato (refusjon $index)")
            }
        }
    }
}

data class BeregningRequest(
    val dagoversikt: List<DagoversiktDagRequest>,
    val refusjon: List<RefusjonInput>,
    val maksdao: Int,
)

data class DagoversiktDagRequest(
    val dato: java.time.LocalDate,
    val dagtype: Dagtype,
    val grad: Int?,
    val yrkesaktivitetId: java.util.UUID,
)
