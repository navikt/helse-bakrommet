package no.nav.helse.bakrommet.kafka

import no.nav.helse.bakrommet.kafka.dto.saksbehandlingsperiode.SaksbehandlingsperiodeKafkaDto
import no.nav.helse.bakrommet.kafka.dto.saksbehandlingsperiode.SaksbehandlingsperiodeStatusKafkaDto
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeReferanse
import no.nav.helse.bakrommet.saksbehandlingsperiode.hentPeriode
import no.nav.helse.bakrommet.saksbehandlingsperiode.somReferanse
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.BeregningResponse
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.UtbetalingsberegningDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetDao
import no.nav.helse.bakrommet.util.HashUtils
import no.nav.helse.bakrommet.util.serialisertTilString

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
    val saksbehandlingsperiodeKafkaDtoMapper =
        SaksbehandlingsperiodeKafkaDtoMapper(
            beregningDao = beregningDao,
            saksbehandlingsperiodeDao = saksbehandlingsperiodeDao,
            sykepengegrunnlagDao = sykepengegrunnlagDao,
            yrkesaktivitetDao = yrkesaktivitetDao,
            personDao = personDao,
        )
    val kafkameldingMedData =
        saksbehandlingsperiodeKafkaDtoMapper.genererKafkaMelding(referanse)
    outboxDao.lagreTilOutbox(kafkameldingMedData.melding)
    if (kafkameldingMedData.saksbehandlingsperiode.status == SaksbehandlingsperiodeStatusKafkaDto.GODKJENT) {
        // TODO sjekk at det faktisk er penger å sende

        kafkameldingMedData.beregning?.let { beregning ->
            outboxDao.lagreTilOutbox(
                KafkaMelding(
                    topic = "speilvendt.sykepenger-spillerom-utbetalinger",
                    key = kafkameldingMedData.melding.key,
                    payload = beregning.beregningData.spilleromOppdrag.serialisertTilString(),
                ),
            )
        }
    }
    /*
    if (kafkameldingMedData.skalHaMeldingOmVedtak()) {
        kafkameldingMedData.beregning?.let { beregning ->
            outboxDao.lagreTilOutbox(
                KafkaMelding(
                    topic = "speilvendt.spillerom-melding-om-vedtak",
                    key = kafkameldingMedData.melding.key,
                    payload = beregning.beregningData.spilleromOppdrag.serialisertTilString(),
                ),
            )
        }
    }

     */
}

data class KafkaMeldingMedData(
    val melding: KafkaMelding,
    val saksbehandlingsperiode: SaksbehandlingsperiodeKafkaDto,
    val beregning: BeregningResponse?,
)

class SaksbehandlingsperiodeKafkaDtoMapper(
    private val beregningDao: UtbetalingsberegningDao,
    private val saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao,
    private val sykepengegrunnlagDao: SykepengegrunnlagDao,
    private val yrkesaktivitetDao: YrkesaktivitetDao,
    private val personDao: PersonDao,
) {
    fun genererKafkaMelding(referanse: SaksbehandlingsperiodeReferanse): KafkaMeldingMedData {
        val periode = saksbehandlingsperiodeDao.hentPeriode(referanse, null)
        val yrkesaktivitet = yrkesaktivitetDao.hentYrkesaktiviteterDbRecord(periode)
        val naturligIdent =
            personDao.hentNaturligIdent(periode.spilleromPersonId)
        val beregning = beregningDao.hentBeregning(referanse.periodeUUID)

        val saksbehandlingsperiodeKafkaDto =
            SaksbehandlingsperiodeKafkaDto(
                id = periode.id,
                fnr = naturligIdent,
                opprettet = periode.opprettet,
                opprettetAvNavIdent = periode.opprettetAvNavIdent,
                opprettetAvNavn = periode.opprettetAvNavn,
                fom = periode.fom,
                tom = periode.tom,
                status = periode.status.tilKafkaDto(),
                beslutterNavIdent = periode.beslutterNavIdent,
                skjæringstidspunkt = periode.skjæringstidspunkt,
                yrkesaktiviteter = emptyList(),
            )

        // Lag sha256 hash av spilleromPersonId som key
        val hash = HashUtils.sha256(periode.spilleromPersonId)
        val kafkaMelding =
            KafkaMelding(
                topic = "speilvendt.spillerom-behandlinger",
                hash,
                saksbehandlingsperiodeKafkaDto.serialisertTilString(),
            )
        return KafkaMeldingMedData(
            melding = kafkaMelding,
            saksbehandlingsperiode = saksbehandlingsperiodeKafkaDto,
            beregning = beregning,
        )
    }
}
