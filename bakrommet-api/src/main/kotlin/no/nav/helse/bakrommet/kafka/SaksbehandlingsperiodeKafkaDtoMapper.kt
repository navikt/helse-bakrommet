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
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagDao
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
        val yrkesaktivitet = yrkesaktivitetDao.hentYrkesaktiviteterDbRecord(periode)
        val naturligIdent =
            personDao.hentNaturligIdent(periode.spilleromPersonId)
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
)

// TODO typ strengt når landet
data class YrkesaktivitetKafkaDto(
    val id: UUID,
    val kategorisering: Map<String, String>,
    val dagoversikt: List<DagKafkaDto>?,
)
