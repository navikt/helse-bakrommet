package no.nav.helse.bakrommet.api.yrkesaktivitet

import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.*
import no.nav.helse.bakrommet.behandling.dagoversikt.Dag
import no.nav.helse.bakrommet.behandling.dagoversikt.Dagtype
import no.nav.helse.bakrommet.behandling.dagoversikt.Kilde

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
        Dagtype.Behandlingsdag -> DagtypeDto.Behandlingsdag
    }

fun DagDto.tilDag(): Dag =
    Dag(
        dato = dato,
        dagtype = dagtype.tilDagtype(),
        grad = grad,
        avslåttBegrunnelse = avslåttBegrunnelse,
        andreYtelserBegrunnelse = andreYtelserBegrunnelse,
        kilde = kilde?.tilKilde(),
    )

fun KildeDto.tilKilde(): Kilde =
    when (this) {
        KildeDto.Søknad -> Kilde.Søknad
        KildeDto.Saksbehandler -> Kilde.Saksbehandler
    }

fun DagtypeDto.tilDagtype(): Dagtype =
    when (this) {
        DagtypeDto.Syk -> Dagtype.Syk
        DagtypeDto.SykNav -> Dagtype.SykNav
        DagtypeDto.Arbeidsdag -> Dagtype.Arbeidsdag
        DagtypeDto.Ferie -> Dagtype.Ferie
        DagtypeDto.Permisjon -> Dagtype.Permisjon
        DagtypeDto.Avslått -> Dagtype.Avslått
        DagtypeDto.AndreYtelser -> Dagtype.AndreYtelser
        DagtypeDto.Behandlingsdag -> Dagtype.Behandlingsdag
    }
