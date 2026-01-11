package no.nav.helse.bakrommet.behandling.yrkesaktivitet

import no.nav.helse.bakrommet.behandling.BehandlingDbRecord
import no.nav.helse.bakrommet.behandling.inntekter.InntektData
import no.nav.helse.bakrommet.behandling.inntekter.InntektRequest
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.Dagoversikt
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.Yrkesaktivitet
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.YrkesaktivitetKategorisering
import no.nav.helse.dto.InntektbeløpDto
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

// Intensjon om at denne kun lever i daoen
data class YrkesaktivitetDbRecord(
    val id: UUID,
    val kategorisering: YrkesaktivitetKategorisering,
    val kategoriseringGenerert: YrkesaktivitetKategorisering?,
    val dagoversikt: Dagoversikt?,
    val dagoversiktGenerert: Dagoversikt?,
    val behandlingId: UUID,
    val opprettet: OffsetDateTime,
    val generertFraDokumenter: List<UUID>,
    val perioder: Perioder? = null,
    val inntektRequest: InntektRequest? = null,
    val inntektData: InntektData? = null,
    val refusjon: List<Refusjonsperiode>? = null,
)

data class YrkesaktivitetForenkletDbRecord(
    val id: UUID,
    val kategorisering: YrkesaktivitetKategorisering,
    val behandlingId: UUID,
)

data class Refusjonsperiode(
    val fom: LocalDate,
    val tom: LocalDate?,
    val beløp: InntektbeløpDto.MånedligDouble,
)

fun YrkesaktivitetDbRecord.tilYrkesaktivitet(): Yrkesaktivitet =
    Yrkesaktivitet(
        id = this.id,
        kategorisering = this.kategorisering,
        kategoriseringGenerert = this.kategoriseringGenerert,
        dagoversikt = this.dagoversikt,
        dagoversiktGenerert = this.dagoversiktGenerert,
        behandlingId = this.behandlingId,
        opprettet = this.opprettet,
        generertFraDokumenter = this.generertFraDokumenter,
        perioder = this.perioder,
        inntektRequest = this.inntektRequest,
        inntektData = this.inntektData,
        refusjon = this.refusjon,
    )

fun Yrkesaktivitet.tilYrkesaktivitetDbRecord(): YrkesaktivitetDbRecord =
    YrkesaktivitetDbRecord(
        id = this.id,
        kategorisering = this.kategorisering,
        kategoriseringGenerert = this.kategoriseringGenerert,
        dagoversikt = this.dagoversikt,
        dagoversiktGenerert = this.dagoversiktGenerert,
        behandlingId = this.behandlingId,
        opprettet = this.opprettet,
        generertFraDokumenter = this.generertFraDokumenter,
        perioder = this.perioder,
        inntektRequest = this.inntektRequest,
        inntektData = this.inntektData,
        refusjon = this.refusjon,
    )

interface YrkesaktivitetDao {
    fun opprettYrkesaktivitet(
        yrkesaktivitetDbRecord: YrkesaktivitetDbRecord,
    ): YrkesaktivitetDbRecord =
        opprettYrkesaktivitet(
            id = yrkesaktivitetDbRecord.id,
            kategorisering = yrkesaktivitetDbRecord.kategorisering,
            dagoversikt = yrkesaktivitetDbRecord.dagoversikt,
            behandlingId = yrkesaktivitetDbRecord.behandlingId,
            opprettet = yrkesaktivitetDbRecord.opprettet,
            generertFraDokumenter = yrkesaktivitetDbRecord.generertFraDokumenter,
            perioder = yrkesaktivitetDbRecord.perioder,
            inntektData = yrkesaktivitetDbRecord.inntektData,
            refusjonsdata = yrkesaktivitetDbRecord.refusjon,
        )

    fun opprettYrkesaktivitet(
        id: UUID,
        kategorisering: YrkesaktivitetKategorisering,
        dagoversikt: Dagoversikt?,
        behandlingId: UUID,
        opprettet: OffsetDateTime,
        generertFraDokumenter: List<UUID>,
        perioder: Perioder?,
        inntektData: InntektData?,
        refusjonsdata: List<Refusjonsperiode>?,
    ): YrkesaktivitetDbRecord

    fun hentYrkesaktivitetDbRecord(id: UUID): YrkesaktivitetDbRecord?

    fun hentYrkesaktivitet(id: UUID): Yrkesaktivitet?

    fun hentYrkesaktiviteter(periode: BehandlingDbRecord): List<Yrkesaktivitet>

    fun hentYrkesaktiviteterDbRecord(periode: BehandlingDbRecord): List<YrkesaktivitetDbRecord>

    fun hentYrkesaktiviteterDbRecord(behandlingId: UUID): List<YrkesaktivitetDbRecord>

    fun oppdaterKategoriseringOgSlettInntektData(
        yrkesaktivitetDbRecord: YrkesaktivitetDbRecord,
        kategorisering: YrkesaktivitetKategorisering,
    ): YrkesaktivitetDbRecord

    fun oppdaterDagoversikt(
        yrkesaktivitetDbRecord: YrkesaktivitetDbRecord,
        oppdatertDagoversikt: Dagoversikt,
    ): YrkesaktivitetDbRecord

    fun oppdaterPerioder(
        yrkesaktivitetDbRecord: YrkesaktivitetDbRecord,
        perioder: Perioder?,
    ): YrkesaktivitetDbRecord

    fun slettYrkesaktivitet(id: UUID)

    fun oppdaterInntektrequest(
        yrkesaktivitet: Yrkesaktivitet,
        request: InntektRequest,
    ): YrkesaktivitetDbRecord

    fun oppdaterInntektData(
        yrkesaktivitet: Yrkesaktivitet,
        inntektData: InntektData,
    ): YrkesaktivitetDbRecord

    fun oppdaterRefusjon(
        yrkesaktivitetID: UUID,
        refusjonsdata: List<Refusjonsperiode>?,
    ): YrkesaktivitetDbRecord

    fun finnYrkesaktiviteterForBehandlinger(map: List<UUID>): List<YrkesaktivitetForenkletDbRecord>
}
