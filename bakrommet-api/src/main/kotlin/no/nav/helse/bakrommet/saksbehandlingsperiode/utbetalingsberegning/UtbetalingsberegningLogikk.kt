package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dag
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dagtype
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagResponse
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Yrkesaktivitet
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
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * Pure function utility for utbetalingsberegning
 * Alle funksjoner er stateless og har ingen sideeffekter
 */
object UtbetalingsberegningLogikk {
    data class YrkesaktivitetMedDekningsgrad(
        val yrkesaktivitet: Yrkesaktivitet,
        val dekningsgrad: Sporbar<ProsentdelDto>?,
    )

    fun beregn(input: UtbetalingsberegningInput): UtbetalingsberegningData {
        val sykepengegrunnlagBegrenset6G = opprettSykepengegrunnlag(input.sykepengegrunnlag)
        val refusjonstidslinjer = opprettRefusjonstidslinjer(input)

        val yrkeskaktivitererMedDekningsgrad =
            input.yrkesaktivitet.map {
                val sykmeldt = it.kategorisering["ER_SYKMELDT"] == "ER_SYKMELDT_JA"
                val dekningsgrad = if (sykmeldt) it.hentDekningsgrad() else null
                YrkesaktivitetMedDekningsgrad(it, dekningsgrad)
            }

        val alleDager = samleAlleDager(yrkeskaktivitererMedDekningsgrad, input.saksbehandlingsperiode)
        val dagBeregninger =
            beregnDagForDag(alleDager, sykepengegrunnlagBegrenset6G, input.sykepengegrunnlag, refusjonstidslinjer)

        return opprettResultat(yrkeskaktivitererMedDekningsgrad, dagBeregninger)
    }

    fun beregnAlaSpleis(input: UtbetalingsberegningInput): List<Pair<Yrkesaktivitet, Utbetalingstidslinje>> {
        val refusjonstidslinjer = opprettRefusjonstidslinjer(input)

        val utbetalingstidslinjer =
            input.yrkesaktivitet.map { ya ->
                val sykdomstidslinje = ya.dagoversikt!!.tilSykdomstidslinje()
                val arbeidsgiverperiode = emptyList<Periode>() // TODO
                val dagerNavOvertarAnsvar = emptyList<Periode>() // TODO
                val refusjonstidslinje =
                    (
                        refusjonstidslinjer[ya.id]?.map { (dato, inntekt) ->
                            Beløpsdag(
                                dato = dato, beløp = inntekt,
                                kilde =
                                    Kilde(
                                        // TODO:
                                        meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()), avsender = Avsender.ARBEIDSGIVER, tidsstempel = LocalDateTime.now(),
                                    ),
                            )
                        } ?: emptyList()
                    ).let {
                        Beløpstidslinje(it)
                    }
                val fastsattÅrsinntekt = finnInntektForYrkesaktivitet(input.sykepengegrunnlag, ya.id)
                val inntektjusteringer = Beløpstidslinje(emptyList()) // TODO ?

                val builder =
                    ArbeidstakerUtbetalingstidslinjeBuilderVedtaksperiode(
                        // TODO : Dekningsgrad er hardkodet til 100% inni Buildern (i og med "Arbeidstaker...Builder")
                        arbeidsgiverperiode = arbeidsgiverperiode,
                        dagerNavOvertarAnsvar = dagerNavOvertarAnsvar,
                        refusjonstidslinje = refusjonstidslinje,
                        fastsattÅrsinntekt = fastsattÅrsinntekt,
                        inntektjusteringer = inntektjusteringer,
                    )

                val utbetalingstidslinje: Utbetalingstidslinje = builder.result(sykdomstidslinje)
                /* Hmmm..... Hvor skjer sammenslåing/fordeling (?)

                YrkesaktivitetUtbetalingsberegning(
                    yrkesaktivitetId = UUID(), dager = listOf(), dekningsgrad = null

                )
                // println(utbetalingstidslinje.toJsonNode().toPrettyString())
                // println(utbetalingstidslinje)

                 */
                utbetalingstidslinje
            }.let {
                Utbetalingstidslinje.totalSykdomsgrad(it)
            }.let {
                Utbetalingstidslinje.betale(
                    sykepengegrunnlagBegrenset6G =
                        Inntekt.gjenopprett(
                            InntektbeløpDto.Årlig(
                                // ?? TODO
                                beløp = minOf(input.sykepengegrunnlag.sykepengegrunnlagØre, input.sykepengegrunnlag.grunnbeløp6GØre) / 100.0,
                            ),
                        ),
                    tidslinjer = it,
                )
            }

        // println(utbetalingstidslinjer.toJsonNode().toPrettyString())

        return input.yrkesaktivitet.zip(utbetalingstidslinjer)
    }

    private fun opprettSykepengegrunnlag(sykepengegrunnlag: SykepengegrunnlagResponse): Inntekt {
        return Inntekt.gjenopprett(
            InntektbeløpDto.Årlig(sykepengegrunnlag.sykepengegrunnlagØre / UtbetalingsberegningKonfigurasjon.ØRE_TIL_KRONER_FAKTOR),
        )
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

    private fun samleAlleDager(
        yrkesaktivitetMedDekningsgrad: List<YrkesaktivitetMedDekningsgrad>,
        saksbehandlingsperiode: Saksbehandlingsperiode,
    ): Map<LocalDate, List<DagMedYrkesaktivitet>> {
        val alleDager = mutableMapOf<LocalDate, MutableList<DagMedYrkesaktivitet>>()

        yrkesaktivitetMedDekningsgrad.forEach { yd ->
            val dagoversikt = yd.yrkesaktivitet.hentDagoversikt()
            val komplettDagoversikt = fyllUtManglendeDager(dagoversikt, saksbehandlingsperiode)

            komplettDagoversikt.forEach { dag ->
                alleDager.getOrPut(dag.dato) { mutableListOf() }.add(
                    DagMedYrkesaktivitet(dag, yd),
                )
            }
        }

        return alleDager
    }

    private fun beregnDagForDag(
        alleDager: Map<LocalDate, List<DagMedYrkesaktivitet>>,
        sykepengegrunnlagBegrenset6G: Inntekt,
        sykepengegrunnlag: SykepengegrunnlagResponse,
        refusjonstidslinjer: Map<UUID, Map<LocalDate, Inntekt>>,
    ): Map<UUID, List<DagUtbetalingsberegning>> {
        val dagBeregningerPerYrkesaktivitet = mutableMapOf<UUID, MutableList<DagUtbetalingsberegning>>()

        alleDager.forEach { (dato, dagerForDato) ->
            val økonomiList =
                dagerForDato.map { dagMedYrkesaktivitet: DagMedYrkesaktivitet ->
                    beregnØkonomiForDag(
                        dag = dagMedYrkesaktivitet.dag,
                        sykepengegrunnlag = sykepengegrunnlag,
                        refusjonstidslinje =
                            refusjonstidslinjer[dagMedYrkesaktivitet.yrkesaktivitet.yrkesaktivitet.id]
                                ?: emptyMap(),
                        yrkesaktivitet = dagMedYrkesaktivitet.yrkesaktivitet.yrkesaktivitet,
                        dekningsgrad = dagMedYrkesaktivitet.yrkesaktivitet.dekningsgrad?.verdi,
                    )
                }

            val økonomiMedTotalGrad = Økonomi.totalSykdomsgrad(økonomiList)
            val beregnedeØkonomier =
                try {
                    Økonomi.betal(sykepengegrunnlagBegrenset6G, økonomiMedTotalGrad)
                } catch (e: IllegalStateException) {
                    // Hvis Økonomi.betal() feiler på grunn av restbeløp, bruk original økonomi
                    økonomiMedTotalGrad
                }

            dagerForDato.zip(beregnedeØkonomier).forEach { (dagMedYrkesaktivitet, beregnetØkonomi) ->
                val yrkesaktivitetId = dagMedYrkesaktivitet.yrkesaktivitet.yrkesaktivitet.id
                val dagBeregning = konverterTilDagBeregning(dagMedYrkesaktivitet.dag, beregnetØkonomi)
                dagBeregningerPerYrkesaktivitet.getOrPut(yrkesaktivitetId) { mutableListOf() }.add(dagBeregning)
            }
        }

        return dagBeregningerPerYrkesaktivitet
    }

    private fun Yrkesaktivitet.hentDagoversikt(): List<Dag> {
        return this.dagoversikt ?: emptyList()
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

    private fun beregnØkonomiForDag(
        dag: Dag,
        sykepengegrunnlag: SykepengegrunnlagResponse,
        refusjonstidslinje: Map<LocalDate, Inntekt>,
        yrkesaktivitet: Yrkesaktivitet,
        dekningsgrad: ProsentdelDto?,
    ): Økonomi {
        val aktuellDagsinntekt = finnInntektForYrkesaktivitet(sykepengegrunnlag, yrkesaktivitet.id)
        val refusjonsbeløp = refusjonstidslinje[dag.dato] ?: Inntekt.INGEN

        // TODO mer eksplisitt håndtering av Syk/SykNav og arbeidsgiverperiode?

        val sykdomsgrad = Sykdomsgrad(dag.grad ?: 0).tilProsentdel()

        return Økonomi.inntekt(
            sykdomsgrad = sykdomsgrad,
            aktuellDagsinntekt = aktuellDagsinntekt,
            // TODO Ikke default til 100% her
            dekningsgrad = dekningsgrad?.tilProsentdel() ?: Prosentdel.HundreProsent,
            refusjonsbeløp = refusjonsbeløp,
            inntektjustering = Inntekt.INGEN,
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

    private fun konverterTilDagBeregning(
        dag: Dag,
        beregnetØkonomi: Økonomi,
    ): DagUtbetalingsberegning {
        // Konverter tilbake til øre-format for output
        // dagligInt returnerer kroner som Int, men vi trenger øre
        val utbetalingØre =
            (
                (
                    beregnetØkonomi.personbeløp?.dagligInt
                        ?: 0
                ) * UtbetalingsberegningKonfigurasjon.ØRE_TIL_KRONER_FAKTOR
            ).toLong()
        val refusjonØre =
            (
                (
                    beregnetØkonomi.arbeidsgiverbeløp?.dagligInt
                        ?: 0
                ) * UtbetalingsberegningKonfigurasjon.ØRE_TIL_KRONER_FAKTOR
            ).toLong()

        // Hent total grad som heltall (som Spleis)
        val totalGrad = beregnetØkonomi.brukTotalGrad { it }

        return DagUtbetalingsberegning(
            dato = dag.dato,
            utbetalingØre = utbetalingØre,
            refusjonØre = refusjonØre,
            totalGrad = totalGrad,
        )
    }

    private fun opprettResultat(
        yrkesaktiviteter: List<YrkesaktivitetMedDekningsgrad>,
        dagBeregninger: Map<UUID, List<DagUtbetalingsberegning>>,
    ): UtbetalingsberegningData {
        val yrkesaktivitetUtbetalingsberegninger =
            yrkesaktiviteter.map { yrkesaktivitet ->
                val dager = dagBeregninger[yrkesaktivitet.yrkesaktivitet.id] ?: emptyList()
                YrkesaktivitetUtbetalingsberegning(
                    yrkesaktivitetId = yrkesaktivitet.yrkesaktivitet.id,
                    dager = dager,
                    dekningsgrad = yrkesaktivitet.dekningsgrad,
                )
            }

        return UtbetalingsberegningData(yrkesaktivitetUtbetalingsberegninger)
    }

    private data class DagMedYrkesaktivitet(
        val dag: Dag,
        val yrkesaktivitet: YrkesaktivitetMedDekningsgrad,
    )
}

fun ProsentdelDto?.tilProsentdel(): Prosentdel? {
    if (this == null) return null
    return Prosentdel.gjenopprett(this)
}
