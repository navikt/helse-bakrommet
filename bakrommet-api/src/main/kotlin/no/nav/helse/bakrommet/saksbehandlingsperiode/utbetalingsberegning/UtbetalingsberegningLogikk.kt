package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dag
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dagtype
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagResponse
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.hentDekningsgrad
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.dto.ProsentdelDto
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.beløp.Beløpsdag
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.utbetalingstidslinje.ArbeidstakerUtbetalingstidslinjeBuilderVedtaksperiode
import no.nav.helse.utbetalingstidslinje.InaktivUtbetalingstidslinjeBuilder
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosentdel
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * Pure function utility for utbetalingsberegning
 * Alle funksjoner er stateless og har ingen sideeffekter
 */
object UtbetalingsberegningLogikk {
    fun beregnAlaSpleis(input: UtbetalingsberegningInput): List<YrkesaktivitetUtbetalingsberegning> {
        val refusjonstidslinjer = opprettRefusjonstidslinjer(input)

        val utbetalingstidslinjer =
            input.yrkesaktivitet.map { ya ->
                val dager = fyllUtManglendeDager(ya.dagoversikt ?: emptyList(), input.saksbehandlingsperiode)

                val arbeidsgiverperiode = ya.arbeidsgiverperioder?.map { Periode.gjenopprett(it) } ?: emptyList()
                val dagerNavOvertarAnsvar: List<Periode> = dager.tilDagerNavOvertarAnsvar()

                // Kast feil hvis dagerNavOvertarAnsvar ikke er inkludert i arbeidsgiverperioden
                if (dagerNavOvertarAnsvar.any { navPeriode -> arbeidsgiverperiode.none { agp -> navPeriode in agp } }) {
                    throw IllegalArgumentException("Ugyldig input: dagerNavOvertarAnsvar må være innenfor arbeidsgiverperioden")
                }

                val sykdomstidslinje = dager.tilSykdomstidslinje(arbeidsgiverperiode)
                val refusjonstidslinje =
                    (
                        refusjonstidslinjer[ya.id]?.map { (dato, inntekt) ->
                            Beløpsdag(
                                dato = dato, beløp = inntekt,
                                kilde =
                                    Kilde(
                                        // TODO:
                                        meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
                                        avsender = Avsender.ARBEIDSGIVER,
                                        tidsstempel = LocalDateTime.now(),
                                    ),
                            )
                        } ?: emptyList()
                    ).let {
                        Beløpstidslinje(it)
                    }
                val fastsattÅrsinntekt = finnInntektForYrkesaktivitet(input.sykepengegrunnlag, ya.id)
                val inntektjusteringer = Beløpstidslinje(emptyList()) // TODO Dette er tilkommen inntekt?

                val dekningsgrad = ya.hentDekningsgrad()

                if (ya.kategorisering["INNTEKTSKATEGORI"] == "INAKTIV") {
                    return@map InaktivUtbetalingstidslinjeBuilder(
                        // TODO : Sporbar dekningsgrad
                        fastsattÅrsinntekt = fastsattÅrsinntekt,
                        dekningsgrad = dekningsgrad.verdi.tilProsentdel(),
                        inntektjusteringer = inntektjusteringer,
                        venteperiode = emptyList(),
                    ).result(sykdomstidslinje)
                }

                // TODO annen builder for næringsdrivende
                // TODO sporbar dekningsgrad
                return@map ArbeidstakerUtbetalingstidslinjeBuilderVedtaksperiode(
                    // TODO : Dekningsgrad er hardkodet til 100% inni Buildern (i og med "Arbeidstaker...Builder")
                    arbeidsgiverperiode = arbeidsgiverperiode,
                    dagerNavOvertarAnsvar = dagerNavOvertarAnsvar,
                    refusjonstidslinje = refusjonstidslinje,
                    fastsattÅrsinntekt = fastsattÅrsinntekt,
                    inntektjusteringer = inntektjusteringer,
                ).result(sykdomstidslinje)
            }.let { utbetalingstidslinjer ->
                // Først beregn total sykdomsgrad på tvers av alle yrkesaktiviteter, dag for dag
                Utbetalingstidslinje.totalSykdomsgrad(utbetalingstidslinjer)
            }.let { utbetalingstidslinjerMedTotalGrad ->
                // Så beregn utbetaling med 6G-begrensning, dag for dag
                val sykepengegrunnlagBegrenset6G =
                    Inntekt.gjenopprett(
                        InntektbeløpDto.Årlig(
                            beløp =
                                minOf(
                                    input.sykepengegrunnlag.sykepengegrunnlagØre,
                                    input.sykepengegrunnlag.grunnbeløp6GØre,
                                ) / 100.0,
                        ),
                    )
                Utbetalingstidslinje.betale(sykepengegrunnlagBegrenset6G, utbetalingstidslinjerMedTotalGrad)
            }

        return input.yrkesaktivitet.zip(utbetalingstidslinjer).map { (yrkesaktivitet, utbetalingstidslinje) ->
            YrkesaktivitetUtbetalingsberegning(
                yrkesaktivitetId = yrkesaktivitet.id,
                utbetalingstidslinje = utbetalingstidslinje,
                dekningsgrad = Sporbar(ProsentdelDto(1.0), Beregningssporing.ARBEIDSTAKER_100),
                // TODO finn riktig dekningsgrad
            )
        }
    }

    private fun opprettRefusjonstidslinjer(input: UtbetalingsberegningInput): Map<UUID, Map<LocalDate, Inntekt>> {
        return input.yrkesaktivitet.associate { yrkesaktivitet ->
            yrkesaktivitet.id to
                RefusjonstidslinjeUtility.beregnRefusjonstidslinje(
                    input.sykepengegrunnlag,
                    yrkesaktivitet.id,
                    input.saksbehandlingsperiode,
                )
        }
    }

    private fun fyllUtManglendeDager(
        eksisterendeDager: List<Dag>,
        saksbehandlingsperiode: Saksbehandlingsperiode,
    ): List<Dag> {
        validerPeriode(saksbehandlingsperiode)

        val eksisterendeDatoer = eksisterendeDager.map { it.dato }.toSet()
        val komplettDagoversikt = mutableListOf<Dag>()

        // Legg til alle eksisterende dager
        komplettDagoversikt.addAll(eksisterendeDager)

        // Fyll ut manglende dager som arbeidsdager
        var aktuellDato = saksbehandlingsperiode.fom
        while (!aktuellDato.isAfter(saksbehandlingsperiode.tom)) {
            if (!eksisterendeDatoer.contains(aktuellDato)) {
                val arbeidsdag = opprettArbeidsdag(aktuellDato)
                komplettDagoversikt.add(arbeidsdag)
            }
            aktuellDato = aktuellDato.plusDays(1)
        }

        // Sorter dager etter dato
        return komplettDagoversikt.sortedBy { it.dato }
    }

    private fun validerPeriode(saksbehandlingsperiode: Saksbehandlingsperiode) {
        if (saksbehandlingsperiode.fom.isAfter(saksbehandlingsperiode.tom)) {
            throw UtbetalingsberegningFeil.UgyldigPeriode(
                saksbehandlingsperiode.fom,
                saksbehandlingsperiode.tom,
            )
        }
    }

    private fun opprettArbeidsdag(dato: LocalDate): Dag {
        return Dag(
            dato = dato,
            dagtype = Dagtype.Arbeidsdag,
            grad = null,
            avslåttBegrunnelse = emptyList(),
            kilde = null,
        )
    }

    private fun finnInntektForYrkesaktivitet(
        sykepengegrunnlag: SykepengegrunnlagResponse,
        yrkesaktivitetId: UUID,
    ): Inntekt {
        val inntekt =
            sykepengegrunnlag.inntekter.find { it.yrkesaktivitetId == yrkesaktivitetId }
                ?: throw UtbetalingsberegningFeil.ManglendeInntekt(yrkesaktivitetId)

        return Inntekt.gjenopprett(
            InntektbeløpDto.Årlig(
                inntekt.beløpPerMånedØre * UtbetalingsberegningKonfigurasjon.MÅNEDLIG_TIL_ÅRLIG_FAKTOR / UtbetalingsberegningKonfigurasjon.ØRE_TIL_KRONER_FAKTOR,
            ),
        )
    }
}

private fun List<Dag>?.tilDagerNavOvertarAnsvar(): List<Periode> {
    // Alle dager som er SykNav er dager NAV overtar ansvar
    if (this == null) return emptyList()
    val sykNavDager = this.filter { it.dagtype == Dagtype.SykNav }.map { it.dato }.toSet()
    if (sykNavDager.isEmpty()) return emptyList()
    val sortedDager = sykNavDager.sorted()
    val perioder = mutableListOf<Periode>()
    var periodeStart = sortedDager.first()
    var periodeSlutt = sortedDager.first()
    for (i in 1 until sortedDager.size) {
        val currentDate = sortedDager[i]
        if (currentDate == periodeSlutt.plusDays(1)) {
            // Fortsett perioden
            periodeSlutt = currentDate
        } else {
            // Avslutt nåværende
            perioder.add(Periode(periodeStart, periodeSlutt))
            // Start ny periode
            periodeStart = currentDate
            periodeSlutt = currentDate
        }
    }
    // Legg til siste periode
    perioder.add(Periode(periodeStart, periodeSlutt))
    return perioder
}

fun ProsentdelDto.tilProsentdel(): Prosentdel {
    return Prosentdel.gjenopprett(this)
}
