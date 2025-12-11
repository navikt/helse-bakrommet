package no.nav.helse.bakrommet.api.yrkesaktivitet

import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.*
import no.nav.helse.bakrommet.behandling.dagoversikt.Kilde
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.Perioder
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.Periodetype
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.Refusjonsperiode
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetMedOrgnavn
import java.time.format.DateTimeFormatter
import no.nav.helse.dto.PeriodeDto as SpleisPeriodeDto

fun YrkesaktivitetMedOrgnavn.tilYrkesaktivitetDto(): YrkesaktivitetDto =
    YrkesaktivitetDto(
        id = yrkesaktivitet.id,
        kategorisering = yrkesaktivitet.kategorisering.tilYrkesaktivitetKategoriseringDto(),
        dagoversikt = yrkesaktivitet.dagoversikt?.map { it.tilDagDto() },
        generertFraDokumenter = yrkesaktivitet.generertFraDokumenter.map { it },
        perioder = yrkesaktivitet.perioder?.tilPerioderDto(),
        inntektRequest = yrkesaktivitet.inntektRequest?.tilInntektRequestDto(),
        inntektData = yrkesaktivitet.inntektData?.tilInntektDataDto(),
        refusjon = yrkesaktivitet.refusjon?.map { it.tilRefusjonsperiodeDto() },
        orgnavn = orgnavn,
    )

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
        beløp = beløp.beløp,
    )
