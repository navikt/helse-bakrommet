package no.nav.helse.bakrommet.kafka

import no.nav.helse.bakrommet.kafka.dto.saksbehandlingsperiode.SaksbehandlingsperiodeKafkaDto
import no.nav.helse.bakrommet.kafka.dto.saksbehandlingsperiode.SaksbehandlingsperiodeStatusKafkaDto
import no.nav.helse.bakrommet.meldingomvedtak.skalHaMeldingOmVedtak
import no.nav.helse.bakrommet.meldingomvedtak.tilMeldingOmVedtak
import no.nav.helse.bakrommet.person.PersonDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeReferanse
import no.nav.helse.bakrommet.saksbehandlingsperiode.hentPeriode
import no.nav.helse.bakrommet.saksbehandlingsperiode.somReferanse
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagDao
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
    val saksbehandlingsperiodeKafkaDto = saksbehandlingsperiodeKafkaDtoMapper.genererKafkaMelding(referanse)
    outboxDao.lagreTilOutbox(
        KafkaMelding(
            topic = "speilvendt.spillerom-behandlinger",
            key = saksbehandlingsperiodeKafkaDto.fnr.tilKafkaKey(),
            saksbehandlingsperiodeKafkaDto.serialisertTilString(),
        ),
    )
    if (saksbehandlingsperiodeKafkaDto.status == SaksbehandlingsperiodeStatusKafkaDto.GODKJENT) {
        // TODO sjekk at det faktisk er penger å sende

        saksbehandlingsperiodeKafkaDto.spilleromOppdrag?.let { spilleromOppdrag ->
            outboxDao.lagreTilOutbox(
                KafkaMelding(
                    topic = "speilvendt.sykepenger-spillerom-utbetalinger",
                    key = spilleromOppdrag.fnr.tilKafkaKey(),
                    payload = spilleromOppdrag.serialisertTilString(),
                ),
            )
        }
    }

    if (saksbehandlingsperiodeKafkaDto.skalHaMeldingOmVedtak()) {
        val meldingOmVedtak = saksbehandlingsperiodeKafkaDto.tilMeldingOmVedtak()
        outboxDao.lagreTilOutbox(
            KafkaMelding(
                topic = "speilvendt.spillerom-melding-om-vedtak",
                key = meldingOmVedtak.fnr.tilKafkaKey(),
                payload = meldingOmVedtak.serialisertTilString(),
            ),
        )
    }
}

fun String.tilKafkaKey(): String = HashUtils.sha256("spillerom-$this")

class SaksbehandlingsperiodeKafkaDtoMapper(
    private val beregningDao: UtbetalingsberegningDao,
    private val saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao,
    private val sykepengegrunnlagDao: SykepengegrunnlagDao,
    private val yrkesaktivitetDao: YrkesaktivitetDao,
    private val personDao: PersonDao,
) {
    fun genererKafkaMelding(referanse: SaksbehandlingsperiodeReferanse): SaksbehandlingsperiodeKafkaDto {
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
                spilleromOppdrag = beregning?.beregningData?.spilleromOppdrag,
            )

        return saksbehandlingsperiodeKafkaDto
    }
}
