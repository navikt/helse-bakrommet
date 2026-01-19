package no.nav.helse.bakrommet.behandling.yrkesaktivitet

import no.nav.helse.bakrommet.behandling.BehandlingDbRecord
import no.nav.helse.bakrommet.behandling.inntekter.InntektDataOld
import no.nav.helse.bakrommet.behandling.inntekter.domain.InntektRequest
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.Dagoversikt
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.LegacyYrkesaktivitet
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
    val inntektData: InntektDataOld? = null,
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

fun YrkesaktivitetDbRecord.tilYrkesaktivitet(): LegacyYrkesaktivitet =
    LegacyYrkesaktivitet(
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

fun LegacyYrkesaktivitet.tilYrkesaktivitetDbRecord(): YrkesaktivitetDbRecord =
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
        inntektData: InntektDataOld?,
        refusjonsdata: List<Refusjonsperiode>?,
    ): YrkesaktivitetDbRecord

    fun hentYrkesaktivitetDbRecord(id: UUID): YrkesaktivitetDbRecord?

    fun hentYrkesaktivitet(id: UUID): LegacyYrkesaktivitet?

    fun hentYrkesaktiviteter(periode: BehandlingDbRecord): List<LegacyYrkesaktivitet>

    fun hentYrkesaktiviteterDbRecord(periode: BehandlingDbRecord): List<YrkesaktivitetDbRecord>

    fun hentYrkesaktiviteterDbRecord(behandlingId: UUID): List<YrkesaktivitetDbRecord>

    fun oppdaterPerioder(
        yrkesaktivitetDbRecord: YrkesaktivitetDbRecord,
        perioder: Perioder?,
    ): YrkesaktivitetDbRecord

    fun oppdaterInntektrequest(
        legacyYrkesaktivitet: LegacyYrkesaktivitet,
        request: InntektRequest,
    ): YrkesaktivitetDbRecord

    fun oppdaterInntektData(
        legacyYrkesaktivitet: LegacyYrkesaktivitet,
        inntektData: InntektDataOld,
    ): YrkesaktivitetDbRecord

    fun oppdaterRefusjon(
        yrkesaktivitetID: UUID,
        refusjonsdata: List<Refusjonsperiode>?,
    ): YrkesaktivitetDbRecord

    fun finnYrkesaktiviteterForBehandlinger(map: List<UUID>): List<YrkesaktivitetForenkletDbRecord>
}
