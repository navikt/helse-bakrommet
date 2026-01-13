package no.nav.helse.bakrommet.kafka

import no.nav.helse.bakrommet.behandling.BehandlingDao
import no.nav.helse.bakrommet.behandling.BehandlingDbRecord
import no.nav.helse.bakrommet.behandling.BehandlingReferanse
import no.nav.helse.bakrommet.behandling.hentPeriode
import no.nav.helse.bakrommet.behandling.somReferanse
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagDao
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.UtbetalingsberegningDao
import no.nav.helse.bakrommet.behandling.vilkaar.VurdertVilkårDao
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetDao
import no.nav.helse.bakrommet.kafka.dto.saksbehandlingsperiode.SaksbehandlingsperiodeKafkaDto
import no.nav.helse.bakrommet.kafka.dto.saksbehandlingsperiode.SaksbehandlingsperiodeStatusKafkaDto
import no.nav.helse.bakrommet.meldingomvedtak.skalHaMeldingOmVedtak
import no.nav.helse.bakrommet.meldingomvedtak.tilMeldingOmVedtak
import no.nav.helse.bakrommet.person.PersonPseudoIdDao
import no.nav.helse.bakrommet.util.HashUtils
import no.nav.helse.bakrommet.util.serialisertTilString

interface SaksbehandlingsperiodeKafkaDtoDaoer {
    val beregningDao: UtbetalingsberegningDao
    val behandlingDao: BehandlingDao
    val sykepengegrunnlagDao: SykepengegrunnlagDao
    val yrkesaktivitetDao: YrkesaktivitetDao
    val personPseudoIdDao: PersonPseudoIdDao
    val outboxDao: OutboxDao
    val vurdertVilkårDao: VurdertVilkårDao
}

fun SaksbehandlingsperiodeKafkaDtoDaoer.leggTilOutbox(periode: BehandlingDbRecord) {
    leggTilOutbox(periode.somReferanse())
}

fun SaksbehandlingsperiodeKafkaDtoDaoer.leggTilOutbox(referanse: BehandlingReferanse) {
    val saksbehandlingsperiodeKafkaDtoMapper =
        SaksbehandlingsperiodeKafkaDtoMapper(
            beregningDao = beregningDao,
            behandlingDao = behandlingDao,
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
    private val behandlingDao: BehandlingDao,
) {
    fun genererKafkaMelding(referanse: BehandlingReferanse): SaksbehandlingsperiodeKafkaDto {
        val periode = behandlingDao.hentPeriode(referanse, null, måVæreUnderBehandling = false)
        val naturligIdent = periode.naturligIdent
        val beregning = beregningDao.hentBeregning(referanse.behandlingId)

        val saksbehandlingsperiodeKafkaDto =
            SaksbehandlingsperiodeKafkaDto(
                id = periode.id,
                fnr = naturligIdent.value,
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
                revurdererSaksbehandlingsperiodeId = periode.revurdererSaksbehandlingsperiodeId,
            )

        return saksbehandlingsperiodeKafkaDto
    }
}
