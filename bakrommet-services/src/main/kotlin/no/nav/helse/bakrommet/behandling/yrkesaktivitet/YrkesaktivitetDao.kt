package no.nav.helse.bakrommet.behandling.yrkesaktivitet

import no.nav.helse.bakrommet.behandling.BehandlingDbRecord
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.LegacyYrkesaktivitet
import no.nav.helse.bakrommet.domain.sykepenger.Dagoversikt
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.InntektData
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.InntektRequest
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.YrkesaktivitetKategorisering
import no.nav.helse.dto.InntektbeløpDto
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

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

data class Refusjonsperiode(
    val fom: LocalDate,
    val tom: LocalDate?,
    val beløp: InntektbeløpDto.MånedligDouble,
)

interface YrkesaktivitetDao {
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

    fun hentYrkesaktiviteter(periode: BehandlingDbRecord): List<LegacyYrkesaktivitet>

    fun hentYrkesaktiviteterDbRecord(periode: BehandlingDbRecord): List<YrkesaktivitetDbRecord>
}
