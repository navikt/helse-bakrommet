package no.nav.helse.bakrommet.api.yrkesaktivitet

import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.*
import no.nav.helse.bakrommet.domain.sykepenger.Dagoversikt
import no.nav.helse.bakrommet.domain.sykepenger.Kilde
import no.nav.helse.bakrommet.domain.sykepenger.Periode
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.Perioder
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.Periodetype
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.Refusjonsperiode

fun Dagoversikt.tilMergetDagoversikt(): List<DagDto> {
    val avslagsdagerMap = this.avslagsdager.associateBy { it.dato }
    return this.sykdomstidlinje.map { dag -> dag.tilDagDto() }.map { dag ->
        val avslagsdag = avslagsdagerMap[dag.dato]
        if (avslagsdag != null) {
            dag.copy(
                dagtype = DagtypeDto.Avslått,
                avslåttBegrunnelse = avslagsdag.avslåttBegrunnelse,
            )
        } else {
            dag
        }
    }
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

fun Periode.tilPeriodeDto(): PeriodeDto =
    PeriodeDto(
        fom = fom,
        tom = tom,
    )

fun Refusjonsperiode.tilRefusjonsperiodeDto(): RefusjonsperiodeDto =
    RefusjonsperiodeDto(
        fom = fom,
        tom = tom,
        beløp = beløp.månedlig,
    )
