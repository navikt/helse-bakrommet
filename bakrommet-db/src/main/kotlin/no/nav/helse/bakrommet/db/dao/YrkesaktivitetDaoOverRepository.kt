package no.nav.helse.bakrommet.db.dao

import no.nav.helse.bakrommet.behandling.BehandlingDbRecord
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.*
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.LegacyYrkesaktivitet
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId
import no.nav.helse.bakrommet.domain.sykepenger.Dagoversikt
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.InntektData
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.Yrkesaktivitetsperiode
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.YrkesaktivitetsperiodeId
import no.nav.helse.bakrommet.repository.YrkesaktivitetsperiodeRepository
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.økonomi.Inntekt
import java.time.OffsetDateTime
import java.util.*
import no.nav.helse.bakrommet.domain.sykepenger.Periode as DomainPeriode
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.Perioder as DomainPerioder
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.Periodetype as DomainPeriodetype
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.Refusjonsperiode as DomainRefusjonsperiode

class YrkesaktivitetDaoOverRepository(
    private val yrkesaktivitetsperiodeRepository: YrkesaktivitetsperiodeRepository,
) : YrkesaktivitetDao {
    override fun opprettYrkesaktivitet(
        id: UUID,
        kategorisering: YrkesaktivitetKategorisering,
        dagoversikt: Dagoversikt?,
        behandlingId: UUID,
        opprettet: OffsetDateTime,
        generertFraDokumenter: List<UUID>,
        perioder: Perioder?,
        inntektData: InntektData?,
        refusjonsdata: List<Refusjonsperiode>?,
    ): YrkesaktivitetDbRecord {
        val yrkesaktivitetsperiode =
            Yrkesaktivitetsperiode(
                id = YrkesaktivitetsperiodeId(id),
                kategorisering = kategorisering,
                kategoriseringGenerert = null,
                dagoversikt = dagoversikt,
                dagoversiktGenerert = null,
                behandlingId = BehandlingId(behandlingId),
                opprettet = opprettet,
                generertFraDokumenter = generertFraDokumenter,
                perioder = perioder?.toDomain(),
                inntektRequest = null,
                inntektData = inntektData,
                refusjon = refusjonsdata?.map { it.toDomain() },
            )
        yrkesaktivitetsperiodeRepository.lagre(yrkesaktivitetsperiode)
        return yrkesaktivitetsperiode.tilDbRecord()
    }

    override fun hentYrkesaktivitetDbRecord(id: UUID): YrkesaktivitetDbRecord? = yrkesaktivitetsperiodeRepository.finn(YrkesaktivitetsperiodeId(id))?.tilDbRecord()

    override fun hentYrkesaktiviteter(periode: BehandlingDbRecord): List<LegacyYrkesaktivitet> =
        yrkesaktivitetsperiodeRepository
            .finn(BehandlingId(periode.id))
            .map { it.tilLegacy() }

    override fun hentYrkesaktiviteterDbRecord(periode: BehandlingDbRecord): List<YrkesaktivitetDbRecord> =
        yrkesaktivitetsperiodeRepository
            .finn(BehandlingId(periode.id))
            .map { it.tilDbRecord() }

    private fun Yrkesaktivitetsperiode.tilDbRecord(): YrkesaktivitetDbRecord =
        YrkesaktivitetDbRecord(
            id = this.id.value,
            kategorisering = this.kategorisering,
            kategoriseringGenerert = this.kategoriseringGenerert,
            dagoversikt = this.dagoversikt,
            dagoversiktGenerert = this.dagoversiktGenerert,
            behandlingId = this.behandlingId.value,
            opprettet = this.opprettet,
            generertFraDokumenter = this.generertFraDokumenter,
            perioder = this.perioder?.toLegacy(),
            inntektRequest = this.inntektRequest,
            inntektData = this.inntektData,
            refusjon = this.refusjon?.map { it.toLegacy() },
        )

    private fun Yrkesaktivitetsperiode.tilLegacy(): LegacyYrkesaktivitet =
        LegacyYrkesaktivitet(
            id = this.id.value,
            kategorisering = this.kategorisering,
            kategoriseringGenerert = this.kategoriseringGenerert,
            dagoversikt = this.dagoversikt,
            dagoversiktGenerert = this.dagoversiktGenerert,
            behandlingId = this.behandlingId.value,
            opprettet = this.opprettet,
            generertFraDokumenter = this.generertFraDokumenter,
            perioder = this.perioder?.toLegacy(),
            inntektRequest = this.inntektRequest,
            inntektData = this.inntektData,
            refusjon = this.refusjon?.map { it.toLegacy() },
        )

    // Mapper fra legacy (behandling.yrkesaktivitet) til domain (domain.sykepenger.yrkesaktivitet)
    private fun Perioder.toDomain(): DomainPerioder =
        DomainPerioder(
            type = this.type.toDomain(),
            perioder = this.perioder.map { DomainPeriode(fom = it.fom, tom = it.tom) },
        )

    private fun Periodetype.toDomain(): DomainPeriodetype =
        when (this) {
            Periodetype.ARBEIDSGIVERPERIODE -> DomainPeriodetype.ARBEIDSGIVERPERIODE
            Periodetype.VENTETID -> DomainPeriodetype.VENTETID
            Periodetype.VENTETID_INAKTIV -> DomainPeriodetype.VENTETID_INAKTIV
        }

    private fun Refusjonsperiode.toDomain(): DomainRefusjonsperiode =
        DomainRefusjonsperiode(
            fom = this.fom,
            tom = this.tom,
            beløp = Inntekt.gjenopprett(this.beløp),
        )

    // Mapper fra domain (domain.sykepenger.yrkesaktivitet) til legacy (behandling.yrkesaktivitet)
    private fun DomainPerioder.toLegacy(): Perioder =
        Perioder(
            type = this.type.toLegacy(),
            perioder =
                this.perioder.map {
                    no.nav.helse.dto
                        .PeriodeDto(fom = it.fom, tom = it.tom)
                },
        )

    private fun DomainPeriodetype.toLegacy(): Periodetype =
        when (this) {
            DomainPeriodetype.ARBEIDSGIVERPERIODE -> Periodetype.ARBEIDSGIVERPERIODE
            DomainPeriodetype.VENTETID -> Periodetype.VENTETID
            DomainPeriodetype.VENTETID_INAKTIV -> Periodetype.VENTETID_INAKTIV
        }

    private fun DomainRefusjonsperiode.toLegacy(): Refusjonsperiode =
        Refusjonsperiode(
            fom = this.fom,
            tom = this.tom,
            beløp = InntektbeløpDto.MånedligDouble(this.beløp.månedlig),
        )
}
