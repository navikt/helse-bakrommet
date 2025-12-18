package no.nav.helse.bakrommet.behandling.yrkesaktivitet

import no.nav.helse.bakrommet.BeregningskoderDekningsgrad
import no.nav.helse.bakrommet.behandling.dagoversikt.Dag
import no.nav.helse.bakrommet.behandling.dagoversikt.Dagtype
import no.nav.helse.bakrommet.behandling.dagoversikt.Kilde
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.Sporbar
import no.nav.helse.bakrommet.behandling.vilkaar.VurdertVilkår
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.Dagoversikt
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.SelvstendigForsikring
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.TypeSelvstendigNæringsdrivende
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.YrkesaktivitetKategorisering
import no.nav.helse.dto.ProsentdelDto

val HUNDRE_PROSENT = ProsentdelDto(1.0)
val ÅTTI_PROSENT = ProsentdelDto(0.8)
val SEKSTIFEM_PROSENT = ProsentdelDto(0.65)

/**
 * Type-sikker versjon av hentDekningsgrad
 */
fun YrkesaktivitetKategorisering.hentDekningsgrad(vilkår: List<VurdertVilkår>): Sporbar<ProsentdelDto> =
    when (this) {
        is YrkesaktivitetKategorisering.SelvstendigNæringsdrivende -> {
            when (val typeSelvstendig = this.typeSelvstendigNæringsdrivende) {
                is TypeSelvstendigNæringsdrivende.Fisker -> {
                    Sporbar(
                        HUNDRE_PROSENT,
                        BeregningskoderDekningsgrad.SELVSTENDIG_KOLLEKTIVFORSIKRING_DEKNINGSGRAD_100,
                    )
                }

                else -> {
                    when (typeSelvstendig.forsikring) {
                        SelvstendigForsikring.FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG ->
                            Sporbar(
                                HUNDRE_PROSENT,
                                BeregningskoderDekningsgrad.SELVSTENDIG_NAVFORSIKRING_DEKNINGSGRAD_100,
                            )

                        SelvstendigForsikring.FORSIKRING_100_PROSENT_FRA_17_SYKEDAG ->
                            Sporbar(
                                HUNDRE_PROSENT,
                                BeregningskoderDekningsgrad.SELVSTENDIG_NAVFORSIKRING_DEKNINGSGRAD_100,
                            )

                        SelvstendigForsikring.FORSIKRING_80_PROSENT_FRA_FØRSTE_SYKEDAG ->
                            Sporbar(ÅTTI_PROSENT, BeregningskoderDekningsgrad.SELVSTENDIG_NAVFORSIKRING_DEKNINGSGRAD_80)

                        SelvstendigForsikring.INGEN_FORSIKRING ->
                            Sporbar(ÅTTI_PROSENT, BeregningskoderDekningsgrad.SELVSTENDIG_DEKNINGSGRAD_80)
                    }
                }
            }
        }

        is YrkesaktivitetKategorisering.Inaktiv -> {
            if (vilkår.any { it.vurdering.underspørsmål.any { underspørsmål -> underspørsmål.svar == "I_ARBEID_UTEN_OPPTJENING" } }) {
                Sporbar(HUNDRE_PROSENT, BeregningskoderDekningsgrad.INAKTIV_DEKNINGSGRAD_100)
            } else {
                Sporbar(SEKSTIFEM_PROSENT, BeregningskoderDekningsgrad.INAKTIV_DEKNINGSGRAD_65)
            }
        }

        is YrkesaktivitetKategorisering.Arbeidstaker ->
            Sporbar(
                HUNDRE_PROSENT,
                BeregningskoderDekningsgrad.ARBEIDSTAKER_DEKNINGSGRAD_100,
            )

        is YrkesaktivitetKategorisering.Frilanser ->
            Sporbar(
                HUNDRE_PROSENT,
                BeregningskoderDekningsgrad.FRILANSER_DEKNINGSGRAD_100,
            )

        is YrkesaktivitetKategorisering.Arbeidsledig ->
            Sporbar(
                HUNDRE_PROSENT,
                BeregningskoderDekningsgrad.ARBEIDSLEDIG_DEKNINGSGRAD_100,
            )
    }

/**
 * Applikerer saksbehandlerens dagoppdateringer på eksisterende dagoversikt.
 * Dager som ikke er avslått oppdaterer eksisterende dager i sykdomstidslinjen.
 * Dager som er avslått legges til eller oppdateres i avslagsdager.
 * Alle oppdaterte dager får kilde satt til Saksbehandler.
 */
fun List<Dag>.applikerSaksbehandlerDagoppdateringer(eksisterendeDagoversikt: Dagoversikt?): Dagoversikt {
    val eksisterendeSykdomstidslinje = eksisterendeDagoversikt?.sykdomstidlinje ?: emptyList()
    val eksisterendeAvslagsdager = eksisterendeDagoversikt?.avslagsdager ?: emptyList()

    val sykdomstidslinjeMap = eksisterendeSykdomstidslinje.associateBy { it.dato }.toMutableMap()
    val avslagsdagerMap = eksisterendeAvslagsdager.associateBy { it.dato }.toMutableMap()

    // Håndter dager som ikke er avslått
    filter { it.dagtype != Dagtype.Avslått }
        .forEach { oppdatertDag ->
            val dato = oppdatertDag.dato
            // Fjern fra avslagsdager hvis den var der før
            avslagsdagerMap.remove(dato)

            // Oppdater sykdomstidslinje hvis dagen eksisterer
            sykdomstidslinjeMap[dato]?.let {
                sykdomstidslinjeMap[dato] = oppdatertDag.copy(kilde = Kilde.Saksbehandler)
            }
        }

    // Håndter avslåtte dager
    filter { it.dagtype == Dagtype.Avslått }
        .forEach { oppdatertDag ->
            val dato = oppdatertDag.dato
            avslagsdagerMap[dato] = oppdatertDag.copy(kilde = Kilde.Saksbehandler)
        }

    return Dagoversikt(
        sykdomstidlinje = sykdomstidslinjeMap.values.toList(),
        avslagsdager = avslagsdagerMap.values.toList(),
    )
}
