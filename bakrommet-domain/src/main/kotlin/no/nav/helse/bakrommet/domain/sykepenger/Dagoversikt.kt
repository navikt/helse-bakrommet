package no.nav.helse.bakrommet.domain.sykepenger

import java.time.LocalDate

data class Dagoversikt(
    val sykdomstidlinje: List<Dag>,
    val avslagsdager: List<Dag>,
) {
    init {
        require(avslagsdager.all { it.dagtype == Dagtype.Avslått }) { "Alle dager i avslagsdager skal være av typen avslagsdag" }
        require(sykdomstidlinje.none { it.dagtype == Dagtype.Avslått }) { "Ingen dager i dagoversikten skal være en avslagsdag" }
        val sykdomsdatoer = sykdomstidlinje.map { it.dato }.toSet()
        require(avslagsdager.all { sykdomsdatoer.contains(it.dato) }) { "Alle dager i avslagsdager skal ha en tilhørende dag i sykdomstidlinje" }
    }

    fun nyDagoversikt(dager: List<Dag>): Dagoversikt {
        val eksisterendeSykdomstidslinje = this.sykdomstidlinje
        val eksisterendeAvslagsdager = this.avslagsdager

        val sykdomstidslinjeMap = eksisterendeSykdomstidslinje.associateBy { it.dato }.toMutableMap()
        val avslagsdagerMap = eksisterendeAvslagsdager.associateBy { it.dato }.toMutableMap()

        // Håndter dager som ikke er avslått
        dager
            .filter { it.dagtype != Dagtype.Avslått }
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
        dager
            .filter { it.dagtype == Dagtype.Avslått }
            .forEach { oppdatertDag ->
                val dato = oppdatertDag.dato
                avslagsdagerMap[dato] = oppdatertDag.copy(kilde = Kilde.Saksbehandler)
            }

        return Dagoversikt(
            sykdomstidlinje = sykdomstidslinjeMap.values.toList(),
            avslagsdager = avslagsdagerMap.values.toList(),
        )
    }

    companion object {
        fun kunArbeidsdager(
            fom: LocalDate,
            tom: LocalDate,
        ): Dagoversikt {
            val dager =
                generateSequence(fom) { it.plusDays(1) }
                    .takeWhile { !it.isAfter(tom) }
                    .map { dato ->
                        Dag(
                            dato = dato,
                            dagtype = Dagtype.Arbeidsdag,
                            grad = null,
                            avslåttBegrunnelse = emptyList(),
                            kilde = Kilde.Saksbehandler,
                        )
                    }.toList()
            return Dagoversikt(sykdomstidlinje = dager, avslagsdager = emptyList())
        }
    }
}
