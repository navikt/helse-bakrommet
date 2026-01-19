package no.nav.helse.bakrommet.api.yrkesaktivitet

import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.DagDto
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.DagtypeDto
import no.nav.helse.bakrommet.domain.sykepenger.Dag
import no.nav.helse.bakrommet.domain.sykepenger.Dagtype


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

