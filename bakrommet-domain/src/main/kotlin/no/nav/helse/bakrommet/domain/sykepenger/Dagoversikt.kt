package no.nav.helse.bakrommet.domain.sykepenger

import java.time.LocalDate

data class Dagoversikt(
    val sykdomstidlinje: List<Dag> = emptyList(),
    val avslagsdager: List<Dag> = emptyList(),
) {
    init {
        require(avslagsdager.all { it.dagtype == Dagtype.Avslått }) { "Alle dager i avslagsdager skal være av typen avslagsdag" }
        require(sykdomstidlinje.none { it.dagtype == Dagtype.Avslått }) { "Ingen dager i dagoversikten skal være en avslagsdag" }
        val sykdomsdatoer = sykdomstidlinje.map { it.dato }.toSet()
        require(avslagsdager.all { sykdomsdatoer.contains(it.dato) }) { "Alle dager i avslagsdager skal ha en tilhørende dag i sykdomstidlinje" }
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
