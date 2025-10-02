package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dag
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dagtype
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Kilde
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Inntekt
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagResponse
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Yrkesaktivitet
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.HashMap
import java.util.UUID
import kotlin.math.min

fun sykepengegrunnlag(inntekter: List<Inntekt>): SykepengegrunnlagResponse {
    val totalInntektØre = 12 * inntekter.sumOf { it.beløpPerMånedØre }
    val grunnbeløpØre = 12402800L
    val grunnbeløp6GØre = 6 * grunnbeløpØre
    return SykepengegrunnlagResponse(
        id = UUID.randomUUID(),
        saksbehandlingsperiodeId = UUID.randomUUID(),
        inntekter = inntekter,
        totalInntektØre = totalInntektØre,
        // 50 000 * 12
        grunnbeløpØre = grunnbeløpØre,
        // 1G
        grunnbeløp6GØre = grunnbeløp6GØre,
        // 6G
        begrensetTil6G = totalInntektØre > grunnbeløp6GØre,
        sykepengegrunnlagØre = min(totalInntektØre, grunnbeløp6GØre),
        grunnbeløpVirkningstidspunkt = LocalDate.of(2024, 5, 1),
        opprettet = "2024-01-01T00:00:00Z",
        opprettetAv = "test",
        sistOppdatert = "2024-01-01T00:00:00Z",
    )
}

fun lagYrkesaktivitet(
    id: UUID = UUID.randomUUID(),
    saksbehandlingsperiodeId: UUID,
    dagoversikt: List<Dag>,
): Yrkesaktivitet {
    return Yrkesaktivitet(
        id = id,
        kategorisering =
            HashMap<String, String>().apply {
                put("INNTEKTSKATEGORI", "ARBEIDSTAKER")
            },
        kategoriseringGenerert = null,
        dagoversikt = dagoversikt,
        dagoversiktGenerert = null,
        saksbehandlingsperiodeId = saksbehandlingsperiodeId,
        opprettet = OffsetDateTime.now(),
        generertFraDokumenter = emptyList(),
    )
}

class DagListeBuilder(førsteDag: LocalDate) {
    val dager = mutableListOf<Dag>()
    private var gjeldendeDag = førsteDag

    fun dag(
        dagtype: Dagtype = Dagtype.Syk,
        grad: Int? = 100,
    ) {
        dager.add(
            Dag(
                dato = gjeldendeDag,
                // I første refusjonsperiode
                dagtype = dagtype,
                grad = grad,
                avslåttBegrunnelse = emptyList(),
                kilde = Kilde.Saksbehandler,
            ),
        )
        gjeldendeDag = gjeldendeDag.plusDays(1)
    }

    fun syk(grad: Int = 100) =
        dag(
            dagtype = Dagtype.Syk,
            grad = grad,
        )
}
