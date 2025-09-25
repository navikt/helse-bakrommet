package no.nav.helse.utbetalingstidslinje


internal interface ArbeidsgiverRegler {
    fun burdeStarteNyArbeidsgiverperiode(oppholdsdagerBrukt: Int): Boolean
    fun arbeidsgiverperiodenGjennomført(arbeidsgiverperiodedagerBrukt: Int): Boolean
    fun maksSykepengedager(): Int
    fun maksSykepengedagerOver67(): Int

    companion object {
        internal object NormalArbeidstaker : ArbeidsgiverRegler {
            private const val arbeidsgiverperiodelengde = 16
            private const val oppholdsdagerFørNyArbeidsgiverperiode = 16

            override fun burdeStarteNyArbeidsgiverperiode(oppholdsdagerBrukt: Int) =
                oppholdsdagerBrukt >= oppholdsdagerFørNyArbeidsgiverperiode

            override fun arbeidsgiverperiodenGjennomført(arbeidsgiverperiodedagerBrukt: Int) =
                arbeidsgiverperiodedagerBrukt >= arbeidsgiverperiodelengde

            override fun maksSykepengedager() = 248
            override fun maksSykepengedagerOver67() = 60
        }
    }
}
