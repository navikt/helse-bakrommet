package no.nav.helse.bakrommet.api.yrkesaktivitet

import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.*
import no.nav.helse.bakrommet.behandling.dagoversikt.Dag
import no.nav.helse.bakrommet.behandling.dagoversikt.Dagtype
import no.nav.helse.bakrommet.behandling.dagoversikt.Kilde
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.Perioder
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.Periodetype
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.Refusjonsperiode
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetDbRecord
import java.time.format.DateTimeFormatter
import no.nav.helse.dto.PeriodeDto as SpleisPeriodeDto

fun YrkesaktivitetDbRecord.tilYrkesaktivitetDto(): YrkesaktivitetDto =
    YrkesaktivitetDto(
        id = id.toString(),
        kategorisering = kategorisering.tilYrkesaktivitetKategoriseringDto(),
        dagoversikt = dagoversikt?.map { it.tilDagDto() },
        generertFraDokumenter = generertFraDokumenter.map { it.toString() },
        perioder = perioder?.tilPerioderDto(),
        inntektRequest = inntektRequest?.tilInntektRequestDto(),
        inntektData = inntektData?.tilInntektDataDto(),
        refusjon = refusjon?.map { it.tilRefusjonsperiodeDto() },
    )

fun Dag.tilDagDto(): DagDto =
    DagDto(
        dato = dato,
        dagtype = dagtype.tilDagtypeDto(),
        grad = grad,
        avslåttBegrunnelse = avslåttBegrunnelse,
        andreYtelserBegrunnelse = andreYtelserBegrunnelse,
        kilde = kilde?.tilKildeDto(),
    )

fun Dagtype.tilDagtypeDto(): DagtypeDto =
    when (this) {
        Dagtype.Syk -> DagtypeDto.Syk
        Dagtype.SykNav -> DagtypeDto.SykNav
        Dagtype.Arbeidsdag -> DagtypeDto.Arbeidsdag
        Dagtype.Ferie -> DagtypeDto.Ferie
        Dagtype.Permisjon -> DagtypeDto.Permisjon
        Dagtype.Avslått -> DagtypeDto.Avslått
        Dagtype.AndreYtelser -> DagtypeDto.AndreYtelser
    }

fun Kilde.tilKildeDto(): KildeDto =
    when (this) {
        Kilde.Søknad -> KildeDto.Søknad
        Kilde.Saksbehandler -> KildeDto.Saksbehandler
    }

fun Perioder.tilPerioderDto(): PerioderDto =
    PerioderDto(
        type = type.tilPeriodetypeDto(),
        perioder = perioder.map { it.tilPeriodeDto() },
    )

fun Periodetype.tilPeriodetypeDto(): PeriodetypeDto =
    when (this) {
        Periodetype.ARBEIDSGIVERPERIODE -> PeriodetypeDto.ARBEIDSGIVERPERIODE
        Periodetype.VENTETID -> PeriodetypeDto.VENTETID
        Periodetype.VENTETID_INAKTIV -> PeriodetypeDto.VENTETID_INAKTIV
    }

fun SpleisPeriodeDto.tilPeriodeDto(): PeriodeDto =
    PeriodeDto(
        fom = fom.format(DateTimeFormatter.ISO_DATE),
        tom = tom.format(DateTimeFormatter.ISO_DATE),
    )

fun Refusjonsperiode.tilRefusjonsperiodeDto(): RefusjonsperiodeDto =
    RefusjonsperiodeDto(
        fom = fom,
        tom = tom,
        beløp = beløp.beløp.toDouble(),
    )
