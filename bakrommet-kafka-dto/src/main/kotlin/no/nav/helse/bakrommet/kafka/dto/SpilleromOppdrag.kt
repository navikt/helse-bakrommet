package no.nav.helse.bakrommet.kafka.dto

import java.time.LocalDate

data class SpilleromOppdrag(
    val spilleromUtbetalingId: String,
    val oppdrag: List<Oppdrag>,
)

data class Oppdrag(
    val mottaker: String,
    val fagområde: Fagområde,
    val linjer: List<Utbetalingslinje>,
)

data class Utbetalingslinje(
    val fom: LocalDate,
    val tom: LocalDate,
    val beløp: Int,
    val grad: Int,
    val klassekode: Klassekode,
)

enum class Klassekode(
    val verdi: String,
) {
    RefusjonIkkeOpplysningspliktig(verdi = "SPREFAG-IOP"),
    SykepengerArbeidstakerOrdinær(verdi = "SPATORD"),
    SelvstendigNæringsdrivendeOppgavepliktig(verdi = "SPSND-OP"),
    SelvstendigNæringsdrivendeFisker(verdi = "SPSNDFISK"),
    SelvstendigNæringsdrivendeJordbrukOgSkogbruk(verdi = "SPSNDJORD"),
    SelvstendigNæringsdrivendeBarnepasserOppgavepliktig(verdi = "SPSNDDM-OP"),
}

enum class Fagområde(
    val verdi: String,
) {
    SykepengerRefusjon("SPREF"),
    Sykepenger("SP"),
}
