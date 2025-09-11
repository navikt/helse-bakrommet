package no.nav.helse.bakrommet.kafka

import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeReferanse
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeStatus
import no.nav.helse.bakrommet.saksbehandlingsperiode.hentPeriode
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.UtbetalingsberegningDao
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Dagoversikt
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Kategorisering
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetDao
import no.nav.helse.bakrommet.util.HashUtils
import no.nav.helse.bakrommet.util.serialisertTilString
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

class SaksbehandlingsperiodeKafkaMeldingMapper(
    private val beregningDao: UtbetalingsberegningDao,
    private val saksbehandlingsperiodeDao: SaksbehandlingsperiodeDao,
    private val sykepengegrunnlagDao: SykepengegrunnlagDao,
    private val yrkesaktivitetDao: YrkesaktivitetDao,
) {
    fun genererKafkaMelding(referanse: SaksbehandlingsperiodeReferanse): KafkaMelding {
        val periode = saksbehandlingsperiodeDao.hentPeriode(referanse, null)
        val yrkesaktivitet = yrkesaktivitetDao.hentYrkesaktivitetFor(periode)
        val saksbehandlingsperiodeKafka =
            SaksbehandlingsperiodeKafka(
                id = periode.id,
                spilleromPersonId = periode.spilleromPersonId,
                opprettet = periode.opprettet,
                opprettetAvNavIdent = periode.opprettetAvNavIdent,
                opprettetAvNavn = periode.opprettetAvNavn,
                fom = periode.fom,
                tom = periode.tom,
                status = periode.status,
                beslutterNavIdent = periode.beslutterNavIdent,
                skjæringstidspunkt = periode.skjæringstidspunkt,
                yrkesaktiviteter =
                    yrkesaktivitet.map {
                        YrkesaktivitetKafka(
                            id = it.id,
                            kategorisering = it.kategorisering,
                            dagoversikt = it.dagoversikt,
                        )
                    },
            )

        // Lag sha256 hash av spilleromPersonId som key
        val hash = HashUtils.sha256(periode.spilleromPersonId)

        return KafkaMelding(hash, saksbehandlingsperiodeKafka.serialisertTilString())
    }
}

data class SaksbehandlingsperiodeKafka(
    val id: UUID,
    val spilleromPersonId: String,
    val opprettet: OffsetDateTime,
    val opprettetAvNavIdent: String,
    val opprettetAvNavn: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val status: SaksbehandlingsperiodeStatus,
    val beslutterNavIdent: String? = null,
    val skjæringstidspunkt: LocalDate? = null,
    val yrkesaktiviteter: List<YrkesaktivitetKafka>,
)

// TODO typ strengt når landet
data class YrkesaktivitetKafka(
    val id: UUID,
    val kategorisering: Kategorisering,
    val dagoversikt: Dagoversikt?,
)
