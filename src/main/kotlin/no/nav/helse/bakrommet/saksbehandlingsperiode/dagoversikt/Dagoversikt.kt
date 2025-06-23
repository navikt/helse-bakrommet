package no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt

import java.time.LocalDate

data class Dag(
    val dato: LocalDate,
    val dagtype: Dagtype,
    val grad: Int?,
    val avvistBegrunnelse: List<AvvistBegrunnelse>,
    val kilde: Kilde,
)

enum class Dagtype {
    Syk,
    SykNav,
    Arbeidsdag,
    Helg,
    Ferie,
    Permisjon,
    Foreldet,
    Avvist,
}

enum class Kilde {
    Søknad,
    Saksbehandler,
}

enum class AvvistBegrunnelse {
    Over70,
    SykepengedagerOppbrukt,
    SykepengedagerOppbruktOver67,
    MinimumInntekt,
    MinimumInntektOver67,
    EgenmeldingUtenforArbeidsgiverperiode,
    MinimumSykdomsgrad,
    ManglerOpptjening,
    ManglerMedlemskap,
    EtterDødsdato,
    AndreYtelserAap,
    AndreYtelserDagpenger,
    AndreYtelserForeldrepenger,
    AndreYtelserOmsorgspenger,
    AndreYtelserOpplaringspenger,
    AndreYtelserPleiepenger,
    AndreYtelserSvangerskapspenger,
    Ukjent,
}
