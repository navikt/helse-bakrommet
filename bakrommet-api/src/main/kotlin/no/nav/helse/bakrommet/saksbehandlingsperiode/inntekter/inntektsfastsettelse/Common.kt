package no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.inntektsfastsettelse

import java.time.YearMonth

internal fun monthsBetween(
    fom: YearMonth,
    tom: YearMonth,
): List<YearMonth> {
    require(!tom.isBefore(fom)) { "tom ($tom) kan ikke være før fom ($fom)" }

    return generateSequence(fom) { prev ->
        if (prev.isBefore(tom)) prev.plusMonths(1) else null
    }.toList()
}
