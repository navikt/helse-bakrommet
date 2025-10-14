package no.nav.helse.bakrommet.kafka

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeReferanse
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeStatus
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dagtype
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Kilde
import no.nav.helse.bakrommet.saksbehandlingsperiode.hentPeriode
import no.nav.helse.bakrommet.saksbehandlingsperiode.somReferanse
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlagold.Inntektskilde
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlagold.SykepengegrunnlagDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlagold.SykepengegrunnlagResponse
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.UtbetalingsberegningDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetDbRecord
import no.nav.helse.bakrommet.util.HashUtils
import no.nav.helse.bakrommet.util.serialisertTilString
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

interface SaksbehandlingsperiodeKafkaDtoDaoer {
    val beregningDao: UtbetalingsberegningDao
    val saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao
    val sykepengegrunnlagDao: SykepengegrunnlagDao
    val yrkesaktivitetDao: YrkesaktivitetDao
    val personDao: PersonDao
    val outboxDao: OutboxDao
}

fun SaksbehandlingsperiodeKafkaDtoDaoer.leggTilOutbox(periode: Saksbehandlingsperiode) {
    leggTilOutbox(periode.somReferanse())
}

fun SaksbehandlingsperiodeKafkaDtoDaoer.leggTilOutbox(referanse: SaksbehandlingsperiodeReferanse) {
    val kafkamelding =
        SaksbehandlingsperiodeKafkaDtoMapper(
            beregningDao = beregningDao,
            saksbehandlingsperiodeDao = saksbehandlingsperiodeDao,
            sykepengegrunnlagDao = sykepengegrunnlagDao,
            yrkesaktivitetDao = yrkesaktivitetDao,
            personDao = personDao,
        ).genererKafkaMelding(referanse)
    outboxDao.lagreTilOutbox(kafkamelding)
}

class SaksbehandlingsperiodeKafkaDtoMapper(
    private val beregningDao: UtbetalingsberegningDao,
    private val saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao,
    private val sykepengegrunnlagDao: SykepengegrunnlagDao,
    private val yrkesaktivitetDao: YrkesaktivitetDao,
    private val personDao: PersonDao,
) {
    fun genererKafkaMelding(referanse: SaksbehandlingsperiodeReferanse): KafkaMelding {
        val periode = saksbehandlingsperiodeDao.hentPeriode(referanse, null)
        val yrkesaktivitet = yrkesaktivitetDao.hentYrkesaktivitetFor(periode)
        val naturligIdent =
            personDao.finnNaturligIdent(periode.spilleromPersonId) ?: throw IllegalStateException("Fant ikke fnr")
        val sykepengegrunnlag = sykepengegrunnlagDao.hentSykepengegrunnlag(referanse.periodeUUID)
        val beregning = beregningDao.hentBeregning(referanse.periodeUUID)

        fun YrkesaktivitetDbRecord.tilYrkesaktivitetKafkaDto(): YrkesaktivitetKafkaDto =
            YrkesaktivitetKafkaDto(
                id = id,
                kategorisering = kategorisering,
                dagoversikt = emptyList(),
                // TODO
            )

        val saksbehandlingsperiodeKafkaDto =
            SaksbehandlingsperiodeKafkaDto(
                id = periode.id,
                spilleromPersonId = periode.spilleromPersonId,
                fnr = naturligIdent,
                opprettet = periode.opprettet,
                opprettetAvNavIdent = periode.opprettetAvNavIdent,
                opprettetAvNavn = periode.opprettetAvNavn,
                fom = periode.fom,
                tom = periode.tom,
                status = periode.status,
                beslutterNavIdent = periode.beslutterNavIdent,
                skjæringstidspunkt = periode.skjæringstidspunkt,
                yrkesaktiviteter = yrkesaktivitet.map { it.tilYrkesaktivitetKafkaDto() },
                sykepengegrunnlag = sykepengegrunnlag.tilSykepengegrunnlagKafkaDto(),
            )

        // Lag sha256 hash av spilleromPersonId som key
        val hash = HashUtils.sha256(periode.spilleromPersonId)
        return KafkaMelding(hash, saksbehandlingsperiodeKafkaDto.serialisertTilString())
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DagKafkaDto(
    val dato: LocalDate,
    val dagtype: Dagtype,
    val refusjonØre: Int?,
    val utbetalingØre: Int?,
    val grad: Int?,
    val totalGrad: Int?,
    val avslåttBegrunnelse: List<String>? = null,
    val andreYtelserBegrunnelse: List<String>? = null,
    val kilde: Kilde?,
)

private fun SykepengegrunnlagResponse?.tilSykepengegrunnlagKafkaDto(): SykepengegrunnlagKafkaDto? {
    if (this == null) return null
    return SykepengegrunnlagKafkaDto(
        inntekter =
            this.inntekter.map {
                InntektKafkaDto(
                    yrkesaktivitetId = it.yrkesaktivitetId,
                    beløpPerMånedØre = it.inntektMånedligØre,
                    kilde = it.kilde,
                    refusjon =
                        it.refusjon.map { ref ->
                            RefusjonsperiodeKafkaDto(
                                fom = ref.fom,
                                tom = ref.tom,
                                beløpØre = ref.beløpØre,
                            )
                        },
                )
            },
        totalInntektØre = this.totalInntektØre,
        grunnbeløpØre = this.grunnbeløpØre,
        grunnbeløp6GØre = this.grunnbeløp6GØre,
        begrensetTil6G = this.begrensetTil6G,
        sykepengegrunnlagØre = this.sykepengegrunnlagØre,
        begrunnelse = this.begrunnelse,
        grunnbeløpVirkningstidspunkt = this.grunnbeløpVirkningstidspunkt,
        opprettet = this.opprettet,
        opprettetAv = this.opprettetAv,
    )
}

data class SaksbehandlingsperiodeKafkaDto(
    val id: UUID,
    val spilleromPersonId: String,
    val fnr: String,
    val opprettet: OffsetDateTime,
    val opprettetAvNavIdent: String,
    val opprettetAvNavn: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val status: SaksbehandlingsperiodeStatus,
    val beslutterNavIdent: String?,
    val skjæringstidspunkt: LocalDate?,
    val yrkesaktiviteter: List<YrkesaktivitetKafkaDto>,
    val sykepengegrunnlag: SykepengegrunnlagKafkaDto?,
)

// TODO typ strengt når landet
data class YrkesaktivitetKafkaDto(
    val id: UUID,
    val kategorisering: Map<String, String>,
    val dagoversikt: List<DagKafkaDto>?,
)

data class InntektKafkaDto(
    val yrkesaktivitetId: UUID,
    val beløpPerMånedØre: Long,
    val kilde: Inntektskilde,
    val refusjon: List<RefusjonsperiodeKafkaDto> = emptyList(),
)

data class RefusjonsperiodeKafkaDto(
    val fom: LocalDate,
    val tom: LocalDate?,
    val beløpØre: Long,
)

data class SykepengegrunnlagKafkaDto(
    val inntekter: List<InntektKafkaDto>,
    val totalInntektØre: Long,
    val grunnbeløpØre: Long,
    val grunnbeløp6GØre: Long,
    val begrensetTil6G: Boolean,
    val sykepengegrunnlagØre: Long,
    val begrunnelse: String? = null,
    val grunnbeløpVirkningstidspunkt: LocalDate,
    val opprettet: String,
    val opprettetAv: String,
)
